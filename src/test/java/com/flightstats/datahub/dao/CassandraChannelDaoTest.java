package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
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
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999));
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.createChannel("foo")).thenReturn(expected);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null);
        ChannelConfiguration result = testClass.createChannel("foo");
        assertEquals(expected, result);
    }

    @Test
    public void testInsert() throws Exception {
        UUID uid = UUID.randomUUID();
        String channelName = "foo";
        byte[] data = "bar".getBytes();

        CassandraInserter inserter = mock(CassandraInserter.class);

        when(inserter.insert(channelName, data)).thenReturn(uid);
        CassandraChannelDao testClass = new CassandraChannelDao(null, inserter);

        UUID result = testClass.insert(channelName, data);

        assertEquals(uid, result);
    }
}
