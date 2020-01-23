package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.webhook.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DynamoWebhookDaoTest {

    @Spy
    private AmazonDynamoDB dbClient;
    @Mock
    private DynamoProperties dynamoProperties;
    @Mock
    private WebhookProperties webhookProperties;

    @Test
    void testSimple() {
        DynamoWebhookDao webhookDao = IntegrationTestSetup.run().getInstance(DynamoWebhookDao.class);
        log.info("DynamoWebhookDao {}", webhookDao);
        assertNotNull(webhookDao);
        Webhook webhook = Webhook.builder()
                .name("testsimple")
                .channelUrl("channelUrl")
                .callbackUrl("callbackUrl")
                .parallelCalls(2)
                .heartbeat(false)
                .paused(false)
                .ttlMinutes(60)
                .maxWaitMinutes(1)
                .maxAttempts(0)
                .callbackTimeoutSeconds(120)
                .batch("MINUTE")
                .build();
        webhookDao.upsert(webhook);

        Webhook testSimple = webhookDao.get("testsimple");
        log.info("webhook {}", testSimple);
        assertNotNull(testSimple);
    }

    @Test
    void testSingleGetReturnsNullIfConfigIsUnparseable() {
        GetItemResult result = mock(GetItemResult.class);
        HashMap<String, AttributeValue> nameMap = new HashMap<>();
        nameMap.put("name", new AttributeValue("bob"));

        when(dynamoProperties.getWebhookConfigTableName()).thenReturn("webhooks");
        when(dbClient.getItem("webhooks", nameMap, true)).thenReturn(result);
        when(result.getItem()).thenReturn(createBogusEntry());

        DynamoWebhookDao mockedDao = new DynamoWebhookDao(dbClient, dynamoProperties, webhookProperties);
        assertNull(mockedDao.get("bob"));
    }

    @Test
    void testListGetDropsUnparseableConfigs() {
        ScanResult result = mock(ScanResult.class);

        Map<String, AttributeValue> webhook1 = toDynamoEntry("webhook1", "callbackUrl1", "channelUrl1");
        Map<String, AttributeValue> webhook2 = toDynamoEntry("webhook2", "callbackUrl2", "channelUrl2");

        Map<String, AttributeValue> bogusRecord = createBogusEntry();

        when(result.getItems()).thenReturn(
                Arrays.asList(
                        webhook1,
                        bogusRecord,
                        webhook2
                )
        );
        when(result.getLastEvaluatedKey()).thenReturn(null);
        when(dbClient.scan(any(ScanRequest.class))).thenReturn(result);
        DynamoWebhookDao mockedDao = new DynamoWebhookDao(dbClient, dynamoProperties, webhookProperties);
        Collection<Webhook> cfgs = mockedDao.getAll(false);
        assertNotNull(cfgs);
        assertEquals(2, cfgs.size());
        assertThat(cfgs, hasItems(fromDynamoEntry(webhook1), fromDynamoEntry(webhook2)));
    }


    @Test
    void testGetAllReturnsMultiplePagesOfResults() {
        Map<String, AttributeValue> webhook1 = toDynamoEntry("webhook1", "callbackUrl1", "channelUrl1");
        ScanResult result = new ScanResult()
                .withLastEvaluatedKey(webhook1)
                .withItems(Collections.singletonList(webhook1));

        Map<String, AttributeValue> webhook2 = toDynamoEntry("webhook2", "callbackUrl2", "channelUrl2");
        ScanResult secondResult = new ScanResult()
                .withItems(Collections.singletonList(webhook2));

        when(dynamoProperties.getWebhookConfigTableName()).thenReturn("webhooks");
        when(dbClient.scan(any(ScanRequest.class))).thenReturn(result, secondResult);

        DynamoWebhookDao webhookDao = new DynamoWebhookDao(dbClient, dynamoProperties, webhookProperties);
        Collection<Webhook> webhookConfigs = webhookDao.getAll(false);

        assertNotNull(webhookConfigs);
        assertEquals(2, webhookConfigs.size());
        assertThat(webhookConfigs, hasItems(fromDynamoEntry(webhook1), fromDynamoEntry(webhook2)));

        ArgumentCaptor<ScanRequest> scanRequest = ArgumentCaptor.forClass(ScanRequest.class);
        verify(dbClient, atLeastOnce()).scan(scanRequest.capture());
        List<ScanRequest> scanRequests = scanRequest.getAllValues();
        assertEquals(2, scanRequests.size());

        ScanRequest firstRequest = scanRequests.get(0);
        assertEquals("webhooks", firstRequest.getTableName());
        assertNull(firstRequest.getExclusiveStartKey());

        ScanRequest secondRequest = scanRequests.get(1);
        assertEquals("webhooks", secondRequest.getTableName());
        assertEquals(webhook1, secondRequest.getExclusiveStartKey());
    }

    private Webhook fromDynamoEntry(Map<String, AttributeValue> dynamoEntry) {
        return Webhook.builder()
                .name(dynamoEntry.get("name").getS())
                .channelUrl(dynamoEntry.get("channelUrl").getS())
                .callbackUrl(dynamoEntry.get("callbackUrl").getS())
                .parallelCalls(Integer.valueOf(dynamoEntry.get("parallelCalls").getN()))
                .heartbeat(dynamoEntry.get("heartbeat").getBOOL())
                .paused(dynamoEntry.get("paused").getBOOL())
                .ttlMinutes(Integer.valueOf(dynamoEntry.get("ttlMinutes").getN()))
                .maxWaitMinutes(Integer.valueOf(dynamoEntry.get("maxWaitMinutes").getN()))
                .maxAttempts(Integer.valueOf(dynamoEntry.get("maxAttempts").getN()))
                .callbackTimeoutSeconds(Integer.valueOf(dynamoEntry.get("callbackTimeoutSeconds").getN()))
                .batch(dynamoEntry.get("batch").getS())
                .build();
    }

    private Map<String, AttributeValue> toDynamoEntry(String name, String callbackUrl, String channelUrl) {
        Map<String, AttributeValue> webhook = new HashMap<>();
        webhook.put("name", new AttributeValue(name));
        webhook.put("callbackUrl", new AttributeValue(callbackUrl));
        webhook.put("channelUrl", new AttributeValue(channelUrl));
        webhook.put("parallelCalls", new AttributeValue().withN("2"));
        webhook.put("heartbeat", new AttributeValue().withBOOL(false));
        webhook.put("paused", new AttributeValue().withBOOL(false));
        webhook.put("ttlMinutes", new AttributeValue().withN("60"));
        webhook.put("maxWaitMinutes", new AttributeValue().withN("60"));
        webhook.put("callbackTimeoutSeconds", new AttributeValue().withN("60"));
        webhook.put("maxAttempts", new AttributeValue().withN("0"));
        webhook.put("batch", new AttributeValue("SINGLE"));
        return webhook;
    }

    private Map<String, AttributeValue> createBogusEntry() {
        Map<String, AttributeValue> bogusRecord = new HashMap<>();
        bogusRecord.put("i exist", new AttributeValue("to break your code"));
        return bogusRecord;
    }

}