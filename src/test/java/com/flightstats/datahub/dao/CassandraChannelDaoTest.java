package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraChannelDaoTest {

    @Test
    public void testChannelExists() throws Exception {
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.channelExists("thechan")).thenReturn(true);
        when(collection.channelExists("nope")).thenReturn(false);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999), null);
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.createChannel("foo")).thenReturn(expected);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null);
        ChannelConfiguration result = testClass.createChannel("foo");
        assertEquals(expected, result);
    }

    @Test
    public void testInsert() throws Exception {
        Date date = new Date(2345678910L);
        DataHubKey key = new DataHubKey(date, (short) 3);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        String contentType = "text/plain";
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
        ValueInsertionResult expected = new ValueInsertionResult(key);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        CassandraValueWriter inserter = mock(CassandraValueWriter.class);

        when(inserter.write(channelName, value)).thenReturn(new ValueInsertionResult(key));
        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, null, null);

        ValueInsertionResult result = testClass.insert(channelName, contentType, data);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        String channelName = "cccccc";
        DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        DataHubCompositeValue expected = new DataHubCompositeValue("text/plain", data);

        CassandraValueReader reader = mock(CassandraValueReader.class);

        when(reader.read(channelName, key)).thenReturn(expected);

        CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader, null);

        LinkedDataHubCompositeValue result = testClass.getValue(channelName, key);
        assertEquals(expected, result); ///FIXME FIXME
    }
}
