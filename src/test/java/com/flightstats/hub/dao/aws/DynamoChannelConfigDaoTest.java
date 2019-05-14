package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.config.DynamoProperties;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.test.Integration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoChannelConfigDaoTest {

    private static final Logger logger = LoggerFactory.getLogger(DynamoChannelConfigDaoTest.class);
    private static DynamoChannelConfigDao channelConfigDao;
    @Mock
    private AmazonDynamoDB dbClient;
    @Mock
    private DynamoProperties dynamoProperties;
    private DynamoChannelConfigDao mockedDao;

    @BeforeAll
    static void setUpClass() throws Exception {
        logger.info("setting up ...");
        Integration.startAwsHub();
        channelConfigDao = HubProvider.getInstance(DynamoChannelConfigDao.class);
    }

    @BeforeEach
    void setup() {
        mockedDao = new DynamoChannelConfigDao(dbClient, dynamoProperties);
    }

    @Test
    void testSimple() {
        logger.info("DynamoChannelConfigDao {}", channelConfigDao);
        assertNotNull(channelConfigDao);
        ChannelConfig channelConfig = ChannelConfig.builder().name("testsimple").build();
        channelConfigDao.upsert(channelConfig);

        ChannelConfig testSimple = channelConfigDao.get("testsimple");
        logger.info("channel {}", testSimple);
        assertNotNull(testSimple);
    }

    @Test
    public void testSingleGetReturnsNullIfConfigIsUnparseable() {
        GetItemResult result = mock(GetItemResult.class);
        when(result.getItem()).thenReturn(createBogusEntry());
        when(dbClient.getItem(any(GetItemRequest.class))).thenReturn(result);
        assertNull(mockedDao.get("bob"));
    }

    @Test
    public void testListGetDropsUnparseableConfigs() {
        ScanResult result = mock(ScanResult.class);

        Map<String, AttributeValue> channel1 = toDynamoEntry("channel1", "Channel One", 1557433692138L);
        Map<String, AttributeValue> channel2 = toDynamoEntry("channel2", "Channel Two", 1557433692138L);

        Map<String,AttributeValue> bogusRecord = createBogusEntry();

        when(result.getItems()).thenReturn(
                Arrays.asList(
                        channel1,
                        bogusRecord,
                        channel2
                )
        );
        when(result.getLastEvaluatedKey()).thenReturn(null);
        when(dbClient.scan(any(ScanRequest.class))).thenReturn(result);
        Collection<ChannelConfig> cfgs = mockedDao.getAll(false);
        assertNotNull(cfgs);
        assertEquals(2, cfgs.size());
        assertThat(cfgs, hasItems(
                channelConfigDao.mapItem(channel1).get(),
                channelConfigDao.mapItem(channel2).get()
        ));
    }

    private Map<String,AttributeValue> toDynamoEntry(String key, String displayName, long date) {
        Map<String,AttributeValue> channel = new HashMap<>();
        channel.put("key", new AttributeValue(key));
        channel.put("displayName", new AttributeValue(displayName));
        channel.put("date", new AttributeValue().withN(String.valueOf(date)));
        return channel;
    }

    private Map<String,AttributeValue> createBogusEntry() {
        Map<String,AttributeValue> bogusRecord = new HashMap<>();
        bogusRecord.put("i exist", new AttributeValue("to break your code"));
        return bogusRecord;
    }

}