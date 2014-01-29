package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.datahub.app.DataHubMain;
import com.flightstats.datahub.cluster.ZooKeeperState;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.dao.ChannelServiceIntegration;
import com.flightstats.datahub.dao.s3.S3ContentDao;
import com.flightstats.datahub.dao.timeIndex.TimeIndex;
import com.flightstats.datahub.dao.timeIndex.TimeIndexProcessor;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.CuratorKeyGenerator;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * This requires DynamoDBLocal to use the dynamo.endpoint localhost:8000
 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
 * start with java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar
 */
public class AwsChannelServiceIntegration extends ChannelServiceIntegration {
    private final static Logger logger = LoggerFactory.getLogger(AwsChannelServiceIntegration.class);

    @BeforeClass
    public static void setupClass() throws Exception {
        Properties properties = DataHubMain.loadProperties("useDefault");
        properties.put("backing.store", "aws");
        finalStartup(properties);
    }

    @Override
    protected void verifyStartup() { }

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
        tearDown();
    }

    //time series isn't supported with the cassandra impl, so it's tested here

    @Test
    public void testTimeSeriesChannelCreation() throws Exception {

        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
    }

    private ChannelConfiguration getChannelConfig(ChannelConfiguration.ChannelType series) {
        return ChannelConfiguration.builder()
                .withName(channelName)
                .withTtlDays(36000L)
                .withType(series)
                .withContentKiloBytes(16)
                .build();
    }

    @Test
    public void testTimeSeriesChannelWriteReadDelete() throws Exception {
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        timeIndexWork(configuration, true);
    }

    @Test
    public void testSequenceTimeIndexChannelWriteReadDelete() throws Exception {
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.Sequence);
        timeIndexWork(configuration, true);
    }

    private void timeIndexWork(ChannelConfiguration configuration, boolean callGetKeys) {
        channelService.createChannel(configuration);

        byte[] bytes = "some data".getBytes();
        ValueInsertionResult insert1 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        ValueInsertionResult insert2 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        ValueInsertionResult insert3 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        HashSet<ContentKey> createdKeys = Sets.newHashSet(insert1.getKey(), insert2.getKey(), insert3.getKey());
        Optional<LinkedContent> value = channelService.getValue(channelName, insert1.getKey().keyToString());
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());

        //this fails using DynamoDBLocal
        if (callGetKeys) {
            DateTime expectedDate = new DateTime(insert1.getDate());
            Collection<ContentKey> futureKeys = channelService.getKeys(channelName, expectedDate.plusYears(1));
            assertEquals(0, futureKeys.size());
            Collection<ContentKey> keys = channelService.getKeys(channelName, expectedDate);
            HashSet<ContentKey> foundKeys = Sets.newHashSet(keys);
            assertEquals(createdKeys, foundKeys);
        }

        channelService.delete(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        channelNames.remove(channelName);
    }

    @Test
    public void testTimeSeriesChannelOptionals() throws Exception {

        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        channelService.createChannel(configuration);
        byte[] bytes = "testChannelOptionals".getBytes();
        Content content = Content.builder().withData(bytes).withContentType("content").withContentLanguage("lang").build();
        ValueInsertionResult insert = channelService.insert(channelName,  content);

        Optional<LinkedContent> value = channelService.getValue(channelName, insert.getKey().keyToString());
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertEquals("content", compositeValue.getContentType().get());
        assertEquals("lang", compositeValue.getValue().getContentLanguage().get());
    }

    @Test
    public void testChannelCreationUncached() throws Exception {
        AmazonDynamoDBClient dbClient = injector.getInstance(AmazonDynamoDBClient.class);
        DynamoUtils dynamoUtils = injector.getInstance(DynamoUtils.class);
        DynamoChannelMetadataDao channelMetadataDao = new DynamoChannelMetadataDao(dbClient, dynamoUtils);
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        ChannelConfiguration channel = channelMetadataDao.createChannel(configuration);
        assertNotNull(channel);
        ChannelConfiguration existing = channelMetadataDao.getChannelConfiguration(configuration.getName());
        assertEquals(configuration.toString(), existing.toString());
    }

    @Test
    public void testUpdateChannelNoChange() throws Exception {
        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
        channelService.updateChannel(configuration);
    }

    @Test
    public void testUpdateChannelChange() throws Exception {
        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.TimeSeries);
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
        ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(configuration)
                .withPeakRequestRate(2)
                .build();
        channelService.updateChannel(newConfig);
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
        S3ContentDao indexDao = new S3ContentDao(keyGenerator, s3Client, "test", curator, metricsTimer);
        TimeIndexProcessor processor = new TimeIndexProcessor(curator, indexDao, new ZooKeeperState());

        DateTime dateTime1 = new DateTime(2014, 1, 6, 12, 45);
        indexDao.writeIndex(channelName, dateTime1, new SequenceContentKey(999));
        DateTime dateTime2 = dateTime1.plusMinutes(1);
        indexDao.writeIndex(channelName, dateTime2, new SequenceContentKey(1000));
        indexDao.writeIndex(channelName, dateTime1.plusMinutes(2), new SequenceContentKey(1001));
        indexDao.writeIndex(channelName, dateTime1.plusMinutes(3), new SequenceContentKey(1002));
        processor.process(channelName);

        assertNull(curator.checkExists().forPath(TimeIndex.getPath(channelName, TimeIndex.getHash(dateTime1))));
        assertNull(curator.checkExists().forPath(TimeIndex.getPath(channelName, TimeIndex.getHash(dateTime2))));

        ArrayList<ContentKey> keyList = Lists.newArrayList(indexDao.getKeys(channelName, dateTime1));
        assertEquals(1, keyList.size());
        assertEquals("999", keyList.get(0).keyToString());

        keyList = Lists.newArrayList(indexDao.getKeys(channelName, dateTime2));
        assertEquals(1, keyList.size());
        assertTrue(keyList.contains(new SequenceContentKey(1000)));
    }
}
