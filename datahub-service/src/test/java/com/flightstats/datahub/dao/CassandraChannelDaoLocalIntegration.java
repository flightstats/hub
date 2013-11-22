package com.flightstats.datahub.dao;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Injector;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * todo - gfm - 11/21/13 - move this code to a different path?
 */
public class CassandraChannelDaoLocalIntegration {
    private final static Logger logger = LoggerFactory.getLogger(CassandraChannelDaoLocalIntegration.class);

    private static Injector injector;
    private ChannelDao channelDao;
    private String channelName;
    private static List<String> channelNames = new ArrayList<>();

    @BeforeClass
    public static void setupClass() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        //todo - gfm - 11/21/13 - these could be in a file, and then we'd need to know where it is
        Properties properties = new Properties();
        properties.put("cassandra.cluster_name", "data hub");
        properties.put("cassandra.hostport", "localhost:9171");
        properties.put("cassandra.replication_factor", "1");
        properties.put("backing.store", "cassandra");
        properties.put("hazelcast.conf.xml", "");
        injector = GuiceContextListenerFactory.construct(properties).getInjector();
    }

    @Before
    public void setUp() throws Exception {
        channelDao = injector.getInstance(ChannelDao.class);
        channelName = UUID.randomUUID().toString();
        channelNames.add(channelName);
    }

    @Test
    public void testChannelCreation() throws Exception {

        assertNull(channelDao.getChannelConfiguration(channelName));
        ChannelConfiguration createdChannel = channelDao.createChannel(channelName, 36000L);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelDao.getChannelConfiguration(channelName));
    }

    @Test
    public void testChannelWriteRead() throws Exception {

        channelDao.createChannel(channelName, 36000L);
        assertFalse(channelDao.getValue(channelName, new DataHubKey(1000)).isPresent());
        byte[] bytes = "some data".getBytes();
        ValueInsertionResult insert = channelDao.insert(channelName, Optional.<String>absent(), Optional.<String>absent(), bytes);

        Optional<LinkedDataHubCompositeValue> value = channelDao.getValue(channelName, insert.getKey());
        assertTrue(value.isPresent());
        LinkedDataHubCompositeValue compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());
    }

    @Test
    public void testChannelOptionals() throws Exception {

        channelDao.createChannel(channelName, 36000L);
        byte[] bytes = "testChannelOptionals".getBytes();
        ValueInsertionResult insert = channelDao.insert(channelName, Optional.of("content"), Optional.of("lang"), bytes);

        Optional<LinkedDataHubCompositeValue> value = channelDao.getValue(channelName, insert.getKey());
        assertTrue(value.isPresent());
        LinkedDataHubCompositeValue compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertEquals("content", compositeValue.getContentType().get());
        assertEquals("lang", compositeValue.getValue().getContentLanguage().get());
    }

    @Test
    public void testChannels() throws Exception {
        Set<String> existing = new HashSet<>(channelNames);
        existing.remove(channelName);
        Set<String> found = new HashSet<>();
        Iterable<ChannelConfiguration> foundChannels = channelDao.getChannels();
        Iterator<ChannelConfiguration> iterator = foundChannels.iterator();
        while (iterator.hasNext()) {
            ChannelConfiguration configuration = iterator.next();
            found.add(configuration.getName());
        }
        logger.info("existing " + existing);
        logger.info("found " + found);
        assertTrue(found.containsAll(existing));

        assertEquals(found.size(), channelDao.countChannels());
    }


}
