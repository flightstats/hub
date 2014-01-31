package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeProvider;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentServiceImplTest {

    private ContentDao contentDao;
    private String channelName;
    private long days;
    private ChannelConfiguration channelConfig;
    private TimeProvider timeProvider;
    private ContentServiceImpl testClass;

    @Before
    public void setUp() throws Exception {
        contentDao = mock(ContentDao.class);

        channelName = "foo";
        days = 90210L;
        channelConfig = ChannelConfiguration.builder().withName(channelName).withTtlDays(days).build();
        timeProvider = mock(TimeProvider.class);
        KeyCoordination keyCoordination = mock(KeyCoordination.class);
        testClass = new ContentServiceImpl(contentDao, timeProvider, keyCoordination);
    }

    @Test
    public void testInsert() throws Exception {
        ContentKey key = new SequenceContentKey( 1003);
        byte[] data = "bar".getBytes();
        Content content = Content.builder().withData(data).withContentType("text/plain").withMillis(days).build();
        ValueInsertionResult expected = new ValueInsertionResult(key, null);

        when(timeProvider.getMillis()).thenReturn(days);
        when(contentDao.write(channelName, content, days)).thenReturn(expected);

        ValueInsertionResult result = testClass.insert(channelConfig, content);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        ContentKey key = new SequenceContentKey( 1001);
        ContentKey previousKey = new SequenceContentKey( 1000);
        ContentKey nextKey = new SequenceContentKey( 1002);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        Content content = Content.builder().withData(data).withContentType("text/plain").withMillis(0L).build();
        Optional<ContentKey> previous = Optional.of(previousKey);
        Optional<ContentKey> next = Optional.of(nextKey);
        LinkedContent expected = new LinkedContent(content, previous, next);

        when(contentDao.read(channelName, key)).thenReturn(content);
        when(contentDao.getKey(key.keyToString())).thenReturn(Optional.of(key));


        Optional<LinkedContent> result = testClass.getValue(channelName, key.keyToString());
        assertEquals(expected, result.get());
    }


}
