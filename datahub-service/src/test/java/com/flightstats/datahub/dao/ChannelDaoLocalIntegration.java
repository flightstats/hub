package com.flightstats.datahub.dao;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Injector;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public abstract class ChannelDaoLocalIntegration {
    private final static Logger logger = LoggerFactory.getLogger(ChannelDaoLocalIntegration.class);

    protected static Injector injector;
    protected ChannelDao channelDao;
    protected String channelName;
    protected static List<String> channelNames = new ArrayList<>();
    private static TestingServer testingServer;


    public static void finalStartup(Properties properties) throws Exception {
        properties.put("zookeeper.connection", "localhost:2181");
        testingServer = new TestingServer(2181);
        injector = GuiceContextListenerFactory.construct(properties).getInjector();
        channelNames.clear();
    }

    @Before
    public void setUp() throws Exception {
        verifyStartup();
        channelDao = injector.getInstance(ChannelDao.class);
        channelName = UUID.randomUUID().toString();
        channelNames.add(channelName);
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        testingServer.stop();
    }

    /**
     * Do any additional verification here
     */
    protected abstract void verifyStartup();

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

        assertTrue(channelDao.isHealthy());
    }

}
