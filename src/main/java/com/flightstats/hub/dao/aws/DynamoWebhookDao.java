package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class DynamoWebhookDao implements Dao<Webhook> {

    private final AmazonDynamoDB dbClient;
    private final DynamoProperties dynamoProperties;
    private final WebhookProperties webhookProperties;

    @Inject
    public DynamoWebhookDao(AmazonDynamoDB dbClient,
                            DynamoProperties dynamoProperties,
                            WebhookProperties webhookProperties) {
        this.dbClient = dbClient;
        this.dynamoProperties = dynamoProperties;
        this.webhookProperties = webhookProperties;
    }

    @Override
    public void upsert(Webhook webhook) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("name", new AttributeValue(webhook.getName()));
        item.put("callbackUrl", new AttributeValue(webhook.getCallbackUrl()));
        if (!StringUtils.isEmpty(webhook.getChannelUrl())) {
            item.put("channelUrl", new AttributeValue(webhook.getChannelUrl()));
        }
        item.put("parallelCalls", new AttributeValue().withN(String.valueOf(webhook.getParallelCalls())));
        item.put("paused", new AttributeValue().withBOOL(webhook.isPaused()));
        item.put("batch", new AttributeValue(webhook.getBatch()));
        item.put("heartbeat", new AttributeValue().withBOOL(webhook.isHeartbeat()));
        item.put("ttlMinutes", new AttributeValue().withN(String.valueOf(webhook.getTtlMinutes())));
        item.put("maxWaitMinutes", new AttributeValue().withN(String.valueOf(webhook.getMaxWaitMinutes())));
        item.put("callbackTimeoutSeconds", new AttributeValue().withN(String.valueOf(webhook.getCallbackTimeoutSeconds())));
        item.put("maxAttempts", new AttributeValue().withN(String.valueOf(webhook.getMaxAttempts())));
        if (!StringUtils.isEmpty(webhook.getErrorChannelUrl())) {
            item.put("errorChannelUrl", new AttributeValue(webhook.getErrorChannelUrl()));
        }
        if (!StringUtils.isEmpty(webhook.getTagUrl())) {
            item.put("tagUrl", new AttributeValue(webhook.getTagUrl()));
        }
        if (!StringUtils.isEmpty(webhook.getManagedByTag())) {
            item.put("tag", new AttributeValue(webhook.getManagedByTag()));
        }
        dbClient.putItem(dynamoProperties.getWebhookConfigTableName(), item);
    }

    @Override
    public Webhook get(String name) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("name", new AttributeValue(name));
        try {
            GetItemResult result = dbClient.getItem(dynamoProperties.getWebhookConfigTableName(), keyMap, true);
            if (result.getItem() == null) {
                return null;
            }
            return tryMapItem(result.getItem())
                    .orElseThrow(() -> new ResourceNotFoundException("Unable to read webhook config from dynamo " + name));
        } catch (ResourceNotFoundException e) {
            log.warn("group not found {}", name, e);
            return null;
        }
    }

    private Optional<Webhook> tryMapItem(Map<String, AttributeValue> item) {
        try {
            return Optional.of(mapItem(item));
        } catch (Exception e) {
            log.warn("Unable to map webhook config {}", item.get("key"), e);
            return Optional.empty();
        }
    }

    private Webhook mapItem(Map<String, AttributeValue> item) {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(item.get("name").getS())
                .callbackUrl(item.get("callbackUrl").getS());
        if (item.containsKey("channelUrl")) {
            builder.channelUrl(item.get("channelUrl").getS());
        }
        if (item.containsKey("parallelCalls")) {
            builder.parallelCalls(Integer.valueOf(item.get("parallelCalls").getN()));
        }
        if (item.containsKey("paused")) {
            builder.paused(item.get("paused").getBOOL());
        }
        if (item.containsKey("batch")) {
            builder.batch(item.get("batch").getS());
        }
        if (item.containsKey("heartbeat")) {
            builder.heartbeat(item.get("heartbeat").getBOOL());
        }
        if (item.containsKey("tag")) {
            builder.managedByTag(item.get("tag").getS());
        }
        if (item.containsKey("ttlMinutes")) {
            builder.ttlMinutes(Integer.valueOf(item.get("ttlMinutes").getN()));
        }
        if (item.containsKey("maxWaitMinutes")) {
            builder.maxWaitMinutes(Integer.valueOf(item.get("maxWaitMinutes").getN()));
        }
        if (item.containsKey("callbackTimeoutSeconds")) {
            builder.callbackTimeoutSeconds(Integer.valueOf(item.get("callbackTimeoutSeconds").getN()));
        }
        if (item.containsKey("tagUrl")) {
            builder.tagUrl(item.get("tagUrl").getS());
        }
        if (item.containsKey("maxAttempts")) {
            builder.maxAttempts(Integer.valueOf(item.get("maxAttempts").getN()));
        }
        if (item.containsKey("errorChannelUrl")) {
            builder.errorChannelUrl(item.get("errorChannelUrl").getS());
        }
        return builder.build().withDefaults(webhookProperties.getCallbackTimeoutDefaultInSec());

    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        List<Webhook> configurations = new ArrayList<>();

        ScanResult result = dbClient.scan(new ScanRequest(dynamoProperties.getWebhookConfigTableName()).withConsistentRead(true));
        mapItems(configurations, result);

        while (result.getLastEvaluatedKey() != null) {
            ScanRequest scanRequest = new ScanRequest(dynamoProperties.getWebhookConfigTableName())
                    .withExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.scan(scanRequest);
            mapItems(configurations, result);
        }

        return configurations;
    }

    private void mapItems(List<Webhook> configurations, ScanResult result) {
        for (Map<String, AttributeValue> item : result.getItems()) {
            tryMapItem(item).ifPresent(configurations::add);
        }
    }

    @Override
    public void delete(String name) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("name", new AttributeValue(name));
        dbClient.deleteItem(new DeleteItemRequest(dynamoProperties.getWebhookConfigTableName(), key));
    }

}