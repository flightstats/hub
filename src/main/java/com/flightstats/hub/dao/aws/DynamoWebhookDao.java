package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DynamoWebhookDao implements Dao<Webhook> {
    private final static Logger logger = LoggerFactory.getLogger(DynamoWebhookDao.class);

    private final AmazonDynamoDB dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoWebhookDao(AmazonDynamoDB dbClient, DynamoUtils dynamoUtils) {
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
        HubServices.register(new DynamoGroupDaoInit());
    }

    private void initialize() {
        if (HubProperties.isReadOnly()) {
            if (!dynamoUtils.doesTableExist(getTableName())) {
                String msg = String.format("Probably fatal error. Dynamo webhook config table doesn't exist for r/o node.  %s", getTableName());
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
            return;
        }
        dynamoUtils.createAndUpdate(getTableName(), "webhook", "name");
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
        dbClient.putItem(getTableName(), item);
    }

    @Override
    public Webhook get(String name) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("name", new AttributeValue(name));
        try {
            GetItemResult result = dbClient.getItem(getTableName(), keyMap, true);
            if (result.getItem() == null) {
                return null;
            }
            return mapItem(result.getItem());
        } catch (ResourceNotFoundException e) {
            logger.info("group not found " + name + " " + e.getMessage());
            return null;
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
        return builder.build().withDefaults();
    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        List<Webhook> configurations = new ArrayList<>();

        ScanResult result = dbClient.scan(new ScanRequest(getTableName()).withConsistentRead(true));
        mapItems(configurations, result);

        while (result.getLastEvaluatedKey() != null) {
            new ScanRequest(getTableName()).setExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.scan(new ScanRequest(getTableName()));
            mapItems(configurations, result);
        }

        return configurations;
    }

    private void mapItems(List<Webhook> configurations, ScanResult result) {
        for (Map<String, AttributeValue> item : result.getItems()) {
            configurations.add(mapItem(item));
        }
    }

    @Override
    public void delete(String name) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("name", new AttributeValue(name));
        dbClient.deleteItem(new DeleteItemRequest(getTableName(), key));
    }

    private String getTableName() {
        String legacyTableName = dynamoUtils.getLegacyTableName("GroupConfig");
        return HubProperties.getProperty("dynamo.table_name.webhook_configs", legacyTableName);
    }

    private class DynamoGroupDaoInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            initialize();
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }
}
