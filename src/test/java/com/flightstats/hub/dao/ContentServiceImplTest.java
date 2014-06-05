package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeProvider;
import com.flightstats.hub.websocket.WebsocketPublisher;
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
        LastUpdatedDao lastUpdatedDao = mock(LastUpdatedDao.class);
        WebsocketPublisher publisher = mock(WebsocketPublisher.class);
        testClass = new ContentServiceImpl(contentDao, lastUpdatedDao, publisher);
    }

    @Test
    public void testInsert() throws Exception {
        ContentKey key = new ContentKey( 1003);
        byte[] data = "bar".getBytes();
        Content content = Content.builder().withData(data).withContentType("text/plain").withMillis(days).build();
        InsertedContentKey expected = new InsertedContentKey(key, null);

        when(timeProvider.getMillis()).thenReturn(days);
        when(contentDao.write(channelName, content, days)).thenReturn(expected);

        InsertedContentKey result = testClass.insert(channelConfig, content);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        ContentKey key = new ContentKey( 1001);
        ContentKey previousKey = new ContentKey( 1000);
        ContentKey nextKey = new ContentKey( 1002);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        Content content = Content.builder().withData(data).withContentType("text/plain").withMillis(0L).build();
        LinkedContent expected = new LinkedContent(content, previousKey, nextKey);

        when(contentDao.read(channelName, key)).thenReturn(content);
        when(contentDao.getKey(key.keyToString())).thenReturn(Optional.of(key));


        Optional<LinkedContent> result = testClass.getValue(channelName, key.keyToString());
        assertEquals(expected, result.get());
    }


}
