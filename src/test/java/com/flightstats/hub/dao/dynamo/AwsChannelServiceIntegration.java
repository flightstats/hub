package com.flightstats.hub.dao.dynamo;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.dao.s3.S3ContentDao;
import com.flightstats.hub.dao.timeIndex.TimeIndex;
import com.flightstats.hub.dao.timeIndex.TimeIndexProcessor;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.model.*;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.CuratorKeyGenerator;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Any test in this class will cause a channel to be expected in testChannels()
 * If the test doesn't create a channel, call channelNames.remove(channelName);
 *
 * AwsChannelServiceIntegration can use DynamoDBLocal to speed up running tests locally.
 * To use it, set dynamo.endpoint=localhost:8000 in src/main/resources/default_local.properties
 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
 * start with java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar
 */
public class AwsChannelServiceIntegration {
    private final static Logger logger = LoggerFactory.getLogger(AwsChannelServiceIntegration.class);

    protected static Injector injector;
    protected ChannelService channelService;
    protected String channelName;
    protected static List<String> channelNames = new ArrayList<>();
    private CuratorFramework curator;

    @BeforeClass
    public static void setupClass() throws Exception {
        injector = Integration.startRealHub();
        channelNames = new ArrayList<>();
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        ChannelService service = injector.getInstance(ChannelService.class);
        for (String channelName : channelNames) {
            try {
                service.delete(channelName);
            } catch (Exception e) {
                logger.warn("unable to delete " + channelName, e);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        channelService = injector.getInstance(ChannelService.class);
        channelName = Integration.getRandomChannel();
        channelNames.add(channelName);
        curator = injector.getInstance(CuratorFramework.class);
    }

    @Test
    public void testChannelCreateDelete() throws Exception {
        channelNames.remove(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
        assertNotNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        channelService.delete(channelName);
        assertNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
    }

    @Test
    public void testChannelWriteReadDelete() throws Exception {
        channelNames.remove(channelName);
        createLocksPath("/TimeIndexLock/");
        createLocksPath("/ChannelReplicator/");
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        Request request = Request.builder().channel(channelName).id(new SequenceContentKey(1000).keyToString()).build();
        assertFalse(channelService.getValue(request).isPresent());
        byte[] bytes = "some data".getBytes();
        Content content = Content.builder().withData(bytes).build();
        InsertedContentKey insert = channelService.insert(channelName, content);
        Request request2 = Request.builder().channel(channelName).id(insert.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request2);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());

        assertNotNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        assertNotNull(curator.checkExists().forPath("/lastUpdated/" + channelName));
        assertNotNull(curator.checkExists().forPath("/TimeIndex/" + channelName));
        assertNotNull(curator.checkExists().forPath("/TimeIndexLock/" + channelName));
        assertNotNull(curator.checkExists().forPath("/ChannelReplicator/" + channelName));
        channelService.delete(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        assertNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        assertNull(curator.checkExists().forPath("/lastUpdated/" + channelName));
        assertNull(curator.checkExists().forPath("/TimeIndex/" + channelName));
        assertNull(curator.checkExists().forPath("/TimeIndexLock/" + channelName));
        //todo - gfm - 4/7/14 - is this still relevant?
        //assertNull(curator.checkExists().forPath("/ChannelReplicator/" + channelName));
    }

    private void createLocksPath(String root) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(root + channelName + "/locks");
        curator.create().creatingParentsIfNeeded().forPath(root + channelName + "/leases");
    }

    @Test
    public void testChannelOptionals() throws Exception {

        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        byte[] bytes = "testChannelOptionals".getBytes();
        Content content = Content.builder().withData(bytes).withContentLanguage("lang").withContentType("content").build();
        InsertedContentKey insert = channelService.insert(channelName, content);

        Request request = Request.builder().channel(channelName).id(insert.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertEquals("content", compositeValue.getContentType().get());
        assertEquals("lang", compositeValue.getValue().getContentLanguage().get());
    }

    /**
     * If this test fails, make sure all new tests either create a channel, or call channelNames.remove(channelName);
     */
    @Test
    public void testChannels() throws Exception {
        Set<String> existing = new HashSet<>(channelNames);
        existing.remove(channelName);
        Set<String> found = new HashSet<>();
        Iterable<ChannelConfiguration> foundChannels = channelService.getChannels();
        Iterator<ChannelConfiguration> iterator = foundChannels.iterator();
        while (iterator.hasNext()) {
            ChannelConfiguration configuration = iterator.next();
            found.add(configuration.getName());
        }
        logger.info("existing " + existing);
        logger.info("found " + found);
        assertTrue(found.containsAll(existing));

        assertTrue(channelService.isHealthy());
    }

    private ChannelConfiguration getChannelConfig(ChannelConfiguration.ChannelType series) {
        return ChannelConfiguration.builder()
                .withName(channelName)
                .withTtlDays(36000L)
                .withType(series)
                .withContentKiloBytes(16)
                .withDescription("descriptive")
                .withTags(Arrays.asList("one", "two", "three"))
                .build();
    }

    @Test
    public void testSequenceTimeIndexChannelWriteReadDelete() throws Exception {
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.Sequence);
        timeIndexWork(configuration, true);
    }

    private void timeIndexWork(ChannelConfiguration configuration, boolean callGetKeys) {
        channelService.createChannel(configuration);

        byte[] bytes = "some data".getBytes();
        InsertedContentKey insert1 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        InsertedContentKey insert2 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        InsertedContentKey insert3 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        HashSet<ContentKey> createdKeys = Sets.newHashSet(insert1.getKey(), insert2.getKey(), insert3.getKey());
        Request request = Request.builder().channel(channelName).id(insert1.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());

        HashSet<ContentKey> foundKeys = Sets.newHashSet();
        //this fails using DynamoDBLocal
        if (callGetKeys) {
            DateTime expectedDate = new DateTime(insert1.getDate());
            foundKeys.addAll(channelService.getKeys(channelName, expectedDate));
            foundKeys.addAll(channelService.getKeys(channelName, expectedDate.plusMinutes(1)));
        }

        assertEquals(createdKeys, foundKeys);

        channelService.delete(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        channelNames.remove(channelName);
    }

    @Test
    public void testAsynchIndices() throws Exception {
        channelNames.remove(channelName);

        //guice private module hate filled
        AmazonS3 s3Client = injector.getInstance(AmazonS3.class);
        CuratorFramework curator = injector.getInstance(CuratorFramework.class);
        MetricsTimer metricsTimer = injector.getInstance(MetricsTimer.class);
        RetryPolicy retryPolicy = injector.getInstance(RetryPolicy.class);
        CuratorKeyGenerator keyGenerator = new CuratorKeyGenerator(curator, metricsTimer, retryPolicy);
        S3ContentDao indexDao = new S3ContentDao(keyGenerator, s3Client, "test", "deihub", 1, 1, false, curator, metricsTimer);
        CuratorLock curatorLock = injector.getInstance(CuratorLock.class);
        TimeIndexProcessor processor = new TimeIndexProcessor(curatorLock, indexDao, curator);

        DateTime dateTime1 = new DateTime(2014, 1, 6, 12, 45);
        indexDao.writeIndex(channelName, dateTime1, new SequenceContentKey(1999));
        DateTime dateTime2 = dateTime1.plusMinutes(1);
        indexDao.writeIndex(channelName, dateTime2, new SequenceContentKey(1000));
        indexDao.writeIndex(channelName, dateTime1.plusMinutes(2), new SequenceContentKey(1001));
        indexDao.writeIndex(channelName, dateTime1.plusMinutes(3), new SequenceContentKey(1002));
        processor.process(channelName);

        assertNull(curator.checkExists().forPath(TimeIndex.getPath(channelName, TimeIndex.getHash(dateTime1))));
        assertNull(curator.checkExists().forPath(TimeIndex.getPath(channelName, TimeIndex.getHash(dateTime2))));

        ArrayList<ContentKey> keyList = Lists.newArrayList(indexDao.getKeys(channelName, dateTime1));
        assertEquals(1, keyList.size());
        assertEquals("1999", keyList.get(0).keyToString());

        keyList = Lists.newArrayList(indexDao.getKeys(channelName, dateTime2));
        assertEquals(1, keyList.size());
        assertTrue(keyList.contains(new SequenceContentKey(1000)));
    }


}
