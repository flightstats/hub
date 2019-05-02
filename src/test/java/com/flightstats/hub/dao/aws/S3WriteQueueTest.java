package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.S3Properties;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.spoke.SpokeWriteContentDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Slf4j
public class S3WriteQueueTest {

    @Mock private SpokeWriteContentDao spokeWriteContentDao;
    @Mock private S3SingleContentDao s3SingleContentDao;
    @Mock private StatsdReporter statsdReporter;
    @Mock private ChannelContentKey key;
    @Mock private ContentKey contentKey;
    @Mock private S3Properties s3Properties;
    private static final String CHANNEL_NAME = "testy_test";
    private static final long AGE_MILLIS = 666;
    private S3WriteQueue s3WriteQueue;

    @Before
    public void setup() {
        initMocks(this);
        when(s3Properties.getWriteQueueSize()).thenReturn(20);
        when(s3Properties.getWriteQueueThreadCount()).thenReturn(2);
        s3WriteQueue = new S3WriteQueue(
                spokeWriteContentDao,
                s3SingleContentDao,
                statsdReporter,
                s3Properties);
    }

    @Test
    public void testS3WriteQueue_add_addsItemToQueue() {
        ChannelContentKey key = keyFactory(1).get(0);
        boolean addedItem = s3WriteQueue.add(key);
        verify(statsdReporter).gauge("s3.writeQueue.used", 1);
        verify(statsdReporter).time("s3.writeQueue.age.added", AGE_MILLIS);
        assertTrue(addedItem);
    }

    @Test
    public void testS3WriteQueue_add_dropsWhenQueueFull() {
        boolean nextAddedItem = keyFactory(21)
                .stream()
                .allMatch(key -> s3WriteQueue.add(key));
        assertEquals(20, s3WriteQueue.getQueueSize());
        verify(statsdReporter).gauge("s3.writeQueue.used", 20);
        verify(statsdReporter, never()).gauge("s3.writeQueue.used", 21);
        verify(statsdReporter).increment( "s3.writeQueue.dropped");

        verify(statsdReporter, times(20)).time("s3.writeQueue.age.added", AGE_MILLIS);
        assertFalse(nextAddedItem);
    }

    @Test
    public void testS3WriteQueue_add_recoversFromQueueFullWithMultipleRuns() {
        // GIVEN
        List<ChannelContentKey> keys = keyFactory(40);

        // WHEN
        keys.forEach(key -> s3WriteQueue.add(key));
        Content content = Content.builder().withContentKey(contentKey).withData(new byte[0]).build();
        when(spokeWriteContentDao.get(any(String.class), any(ContentKey.class))).thenReturn(content);
        keys.subList(0, 20).forEach(key -> tryWrite());


        // THEN
        assertEquals(0, s3WriteQueue.getQueueSize());

        // WHEN
        keys.subList(20, 40).forEach(key -> s3WriteQueue.add(key));

        // THEN
        assertEquals(20, s3WriteQueue.getQueueSize());
        verify(statsdReporter, times(40)).time("s3.writeQueue.age.added", AGE_MILLIS);
    }

    private List<ChannelContentKey> keyFactory(int keyCount) {
        List<ChannelContentKey> keys = new ArrayList<>();
        for (int i = 1; i <= keyCount; i++) {
            ContentKey contentKey = ContentKey.fromFullUrl("http://hubbi/channel/" + CHANNEL_NAME + "/2020/05/01/01/01/01/000/zvp3r" + i);
            when(key.getAgeMS()).thenReturn(AGE_MILLIS);
            when(key.getChannel()).thenReturn(CHANNEL_NAME);
            when(key.getContentKey()).thenReturn(contentKey);
            keys.add(key);
        }
        return keys;
    }

    private void tryWrite() {
        try {
            s3WriteQueue.write();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}