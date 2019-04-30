package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.spoke.SpokeWriteContentDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Slf4j
public class S3WriteQueueTest {

    private static final String CHANNEL_NAME = "testy_test";
    private static final long AGE_MILLIS = 666;

    @Mock
    private SpokeWriteContentDao spokeWriteContentDao;
    @Mock
    private S3SingleContentDao s3SingleContentDao;
    @Mock
    private StatsdReporter statsdReporter;
    @Mock
    private ChannelContentKey key;
    @Mock
    private ContentKey contentKey;
    @Mock
    private S3Property s3Property;
    private S3WriteQueue s3WriteQueue;

    @Before
    public void setup() {
        initMocks(this);

        when(s3Property.getWriteQueueSize()).thenReturn(20);
        when(s3Property.getWriteQueueThreadCount()).thenReturn(2);

        s3WriteQueue = new S3WriteQueue(
                spokeWriteContentDao,
                s3SingleContentDao,
                statsdReporter,
                s3Property);

    }

    @Test
    public void testS3WriteQueue_add_addsItemToQueue() {
        ChannelContentKey key = keyFactory(1).get(0);
        boolean addedItem = s3WriteQueue.add(key);
        verify(statsdReporter).gauge("s3.writeQueue.used", 1);
        verify(statsdReporter).time("s3.writeQueue.age.added", AGE_MILLIS);
        assertTrue(addedItem);
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
