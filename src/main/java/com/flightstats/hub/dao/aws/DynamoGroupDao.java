package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupDao;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoGroupDao implements GroupDao {
    private final static Logger logger = LoggerFactory.getLogger(DynamoGroupDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoGroupDao(AmazonDynamoDBClient dbClient, DynamoUtils dynamoUtils) {
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
        HubServices.register(new DynamoGroupDaoInit());
    }

    private void initialize() {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(getTableName())
                .withAttributeDefinitions(new AttributeDefinition("name", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("name", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(50L, 10L));
        dynamoUtils.createTable(request);
    }

    @Override
    public Group upsertGroup(Group group) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("name", new AttributeValue(group.getName()));
        item.put("callbackUrl", new AttributeValue(group.getCallbackUrl()));
        item.put("channelUrl", new AttributeValue(group.getChannelUrl()));
        item.put("parallelCalls", new AttributeValue().withN(String.valueOf(group.getParallelCalls())));
        item.put("paused", new AttributeValue().withBOOL(group.isPaused()));
        item.put("batch", new AttributeValue(group.getBatch()));
        item.put("heartbeat", new AttributeValue().withBOOL(group.isHeartbeat()));
        item.put("ttlMinutes", new AttributeValue().withN(String.valueOf(group.getTtlMinutes())));
        item.put("maxWaitMinutes", new AttributeValue().withN(String.valueOf(group.getMaxWaitMinutes())));
        dbClient.putItem(getTableName(), item);
        return group;
    }

    @Override
    public Optional<Group> getGroup(String name) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("name", new AttributeValue(name));
        try {
            GetItemResult result = dbClient.getItem(getTableName(), keyMap, true);
            if (result.getItem() == null) {
                return Optional.absent();
            }
            return Optional.of(mapItem(result.getItem()));
        } catch (ResourceNotFoundException e) {
            logger.info("group not found " + name + " " + e.getMessage());
            return Optional.absent();
        }
    }

    private Group mapItem(Map<String, AttributeValue> item) {
        Group.GroupBuilder groupBuilder = Group.builder()
                .name(item.get("name").getS())
                .callbackUrl(item.get("callbackUrl").getS())
                .channelUrl(item.get("channelUrl").getS());
        if (item.containsKey("parallelCalls")) {
            groupBuilder.parallelCalls(Integer.valueOf(item.get("parallelCalls").getN()));
        }
        if (item.containsKey("paused")) {
            groupBuilder.paused(item.get("paused").getBOOL());
        }
        if (item.containsKey("batch")) {
            groupBuilder.batch(item.get("batch").getS());
        }
        if (item.containsKey("heartbeat")) {
            groupBuilder.heartbeat(item.get("heartbeat").getBOOL());
        }
        if (item.containsKey("ttlMinutes")) {
            groupBuilder.ttlMinutes(Integer.valueOf(item.get("ttlMinutes").getN()));
        }
        if (item.containsKey("maxWaitMinutes")) {
            groupBuilder.maxWaitMinutes(Integer.valueOf(item.get("maxWaitMinutes").getN()));
        }
        return groupBuilder.build().withDefaults(false);
    }

    @Override
    public Iterable<Group> getGroups() {
        List<Group> configurations = new ArrayList<>();

        ScanResult result = dbClient.scan(new ScanRequest(getTableName()).withConsistentRead(true));
        mapItems(configurations, result);

        while (result.getLastEvaluatedKey() != null) {
            new ScanRequest(getTableName()).setExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.scan(new ScanRequest(getTableName()));
            mapItems(configurations, result);
        }

        return configurations;
    }

    private void mapItems(List<Group> configurations, ScanResult result) {
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
        return dynamoUtils.getTableName("GroupConfig");
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
