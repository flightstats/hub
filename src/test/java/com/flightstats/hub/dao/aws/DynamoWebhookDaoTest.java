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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DynamoWebhookDaoTest {
    @Mock
    private AmazonDynamoDB dbClient;
    @Mock
    private DynamoProperties dynamoProperties;
    @Mock
    private WebhookProperties webhookProperties;

    @Test
    void testSimple() {
        DynamoWebhookDao webhookDao = IntegrationTestSetup.run().getInstance(DynamoWebhookDao.class);
        assertNotNull(webhookDao);
        Webhook webhook = buildGenericWebhook("testsimple");
        webhookDao.upsert(webhook);

        Webhook testSimple = webhookDao.get("testsimple");
        assertNotNull(testSimple);
    }

    @Test
    void testGetAllReturnsMultiplePagesOfResults() {
        when(dynamoProperties.getWebhookConfigTableName()).thenReturn("webhooks");

        Webhook webhook1 = buildGenericWebhook("webhook1");
        Webhook webhook2 = buildGenericWebhook("webhook2");

        Map<String, AttributeValue> webhook1Record = toDynamoEntry(webhook1);
        Map<String, AttributeValue> webhook2Record = toDynamoEntry(webhook2);

        ScanResult result = new ScanResult()
                .withLastEvaluatedKey(webhook1Record)
                .withItems(Collections.singletonList(webhook1Record));
        ScanResult secondResult = new ScanResult()
                .withItems(Collections.singletonList(webhook2Record));
        when(dbClient.scan(any(ScanRequest.class))).thenReturn(result, secondResult);

        DynamoWebhookDao webhookDao = new DynamoWebhookDao(dbClient, dynamoProperties, webhookProperties);
        Collection<Webhook> webhookConfigs = webhookDao.getAll(false);

        assertNotNull(webhookConfigs);
        assertEquals(2, webhookConfigs.size());
        assertThat(webhookConfigs, hasItems(webhook1, webhook2));

        ArgumentCaptor<ScanRequest> scanRequest = ArgumentCaptor.forClass(ScanRequest.class);
        verify(dbClient, atLeastOnce()).scan(scanRequest.capture());
        List<ScanRequest> scanRequests = scanRequest.getAllValues();
        assertEquals(2, scanRequests.size());

        ScanRequest firstRequest = scanRequests.get(0);
        assertEquals("webhooks", firstRequest.getTableName());
        assertNull(firstRequest.getExclusiveStartKey());

        ScanRequest secondRequest = scanRequests.get(1);
        assertEquals("webhooks", secondRequest.getTableName());
        assertEquals(webhook1Record, secondRequest.getExclusiveStartKey());
    }

    private Webhook buildGenericWebhook(String name) {
        return Webhook.builder()
                .name(name)
                .channelUrl(name + "ChannelUrl")
                .callbackUrl(name + "CallbackUrl")
                .parallelCalls(2)
                .heartbeat(false)
                .paused(false)
                .ttlMinutes(60)
                .maxWaitMinutes(1)
                .maxAttempts(0)
                .callbackTimeoutSeconds(120)
                .batch("SINGLE")
                .build();
    }

    private Map<String, AttributeValue> toDynamoEntry(Webhook webhook) {
        Function<Integer, AttributeValue> getNumericAttributeValue = number ->
                new AttributeValue().withN(String.valueOf(number));

        Map<String, AttributeValue> dynamoEntry = new HashMap<>();
        dynamoEntry.put("name", new AttributeValue(webhook.getName()));
        dynamoEntry.put("callbackUrl", new AttributeValue(webhook.getCallbackUrl()));
        dynamoEntry.put("channelUrl", new AttributeValue(webhook.getChannelUrl()));
        dynamoEntry.put("parallelCalls", getNumericAttributeValue.apply(webhook.getParallelCalls()));
        dynamoEntry.put("heartbeat", new AttributeValue().withBOOL(webhook.isHeartbeat()));
        dynamoEntry.put("paused", new AttributeValue().withBOOL(webhook.isPaused()));
        dynamoEntry.put("ttlMinutes", getNumericAttributeValue.apply(webhook.getTtlMinutes()));
        dynamoEntry.put("maxWaitMinutes", getNumericAttributeValue.apply(webhook.getMaxWaitMinutes()));
        dynamoEntry.put("callbackTimeoutSeconds", getNumericAttributeValue.apply(webhook.getCallbackTimeoutSeconds()));
        dynamoEntry.put("maxAttempts", getNumericAttributeValue.apply(webhook.getMaxAttempts()));
        dynamoEntry.put("batch", new AttributeValue(webhook.getBatch()));
        return dynamoEntry;
    }
}