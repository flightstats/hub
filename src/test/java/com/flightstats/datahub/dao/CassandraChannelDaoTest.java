package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ValueInsertionResult;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

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
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999));
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.createChannel("foo")).thenReturn(expected);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null);
        ChannelConfiguration result = testClass.createChannel("foo");
        assertEquals(expected, result);
    }

    @Test
    public void testInsert() throws Exception {
        UUID uid = UUID.randomUUID();
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Date date = new Date(2345678910L);
        ValueInsertionResult expected = new ValueInsertionResult(uid, date);

        CassandraValueWriter inserter = mock(CassandraValueWriter.class);

        when(inserter.write(channelName, data)).thenReturn(new ValueInsertionResult(uid, date));
        CassandraChannelDao testClass = new CassandraChannelDao(null, inserter, null);

        ValueInsertionResult result = testClass.insert(channelName, data);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        String channelName = "cccccc";
        UUID uid = UUID.randomUUID();
        byte[] expected = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

        CassandraValueReader reader = mock(CassandraValueReader.class);

        when(reader.read(channelName, uid)).thenReturn(expected);

        CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader);

        byte[] result = testClass.getValue(channelName, uid);
        assertEquals(expected, result);
    }
}
