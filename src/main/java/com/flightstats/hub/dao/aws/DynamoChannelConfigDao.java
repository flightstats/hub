package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.GlobalConfig;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DynamoChannelConfigDao implements Dao<ChannelConfig> {
    private final static Logger logger = LoggerFactory.getLogger(DynamoChannelConfigDao.class);

    @Inject
    private AmazonDynamoDBClient dbClient;
    @Inject
    private DynamoUtils dynamoUtils;

    @Inject
    public DynamoChannelConfigDao() {
        HubServices.register(new DynamoChannelConfigurationDaoInit());
    }

    @Override
    public void upsert(ChannelConfig config) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue(config.getName()));
        item.put("date", new AttributeValue().withN(String.valueOf(config.getCreationDate().getTime())));
        item.put("ttlDays", new AttributeValue().withN(String.valueOf(config.getTtlDays())));
        if (!config.getTags().isEmpty()) {
            item.put("tags", new AttributeValue().withSS(config.getTags()));
        }
        if (StringUtils.isNotEmpty(config.getDescription())) {
            item.put("description", new AttributeValue(config.getDescription()));
        }
        if (StringUtils.isNotEmpty(config.getReplicationSource())) {
            item.put("replicationSource", new AttributeValue(config.getReplicationSource()));
        }
        item.put("maxItems", new AttributeValue().withN(String.valueOf(config.getMaxItems())));
        if (StringUtils.isNotEmpty(config.getOwner())) {
            item.put("owner", new AttributeValue(config.getOwner()));
        }
        if (StringUtils.isNotEmpty(config.getStorage())) {
            item.put("storage", new AttributeValue(config.getStorage()));
        }
        if (config.isGlobal()) {
            GlobalConfig global = config.getGlobal();
            item.put("master", new AttributeValue(global.getMaster()));
            item.put("satellites", new AttributeValue().withSS(global.getSatellites()));
            item.put("isMaster", new AttributeValue().withBOOL(global.isMaster()));
        }
        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName())
                .withItem(item);
        dbClient.putItem(putItemRequest);
    }

    private void initialize() {
        createTable();
    }

    private void createTable() {
        long readThroughput = HubProperties.getProperty("dynamo.throughput.channel.read", 100);
        long writeThroughput = HubProperties.getProperty("dynamo.throughput.channel.write", 10);
        logger.info("creating table {} with read {} and write {}", getTableName(), readThroughput, writeThroughput);
        ProvisionedThroughput throughput = new ProvisionedThroughput(readThroughput, writeThroughput);
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(getTableName())
                .withAttributeDefinitions(new AttributeDefinition("key", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("key", KeyType.HASH))
                .withProvisionedThroughput(throughput);
        dynamoUtils.createTable(request);
        dynamoUtils.updateTable(getTableName(), throughput);
    }

    @Override
    public boolean exists(String name) {
        return getCached(name) != null;
    }

    @Override
    public ChannelConfig getCached(String name) {
        return get(name);
    }

    @Override
    public ChannelConfig get(String name) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("key", new AttributeValue().withS(name));
        GetItemRequest getItemRequest = new GetItemRequest()
                .withConsistentRead(true)
                .withTableName(getTableName())
                .withKey(keyMap);
        try {
            GetItemResult result = dbClient.getItem(getItemRequest);
            if (result.getItem() == null) {
                return null;
            }
            return mapItem(result.getItem());
        } catch (ResourceNotFoundException e) {
            logger.info("channel not found " + e.getMessage());
            return null;
        }
    }

    private ChannelConfig mapItem(Map<String, AttributeValue> item) {
        ChannelConfig.Builder builder = ChannelConfig.builder()
                .withCreationDate(new Date(Long.parseLong(item.get("date").getN())))
                .withName(item.get("key").getS());
        if (item.get("ttlDays") != null) {
            builder.withTtlDays(Long.parseLong(item.get("ttlDays").getN()));
        }
        if (item.containsKey("description")) {
            builder.withDescription(item.get("description").getS());
        }
        if (item.containsKey("tags")) {
            builder.withTags(item.get("tags").getSS());
        }
        if (item.containsKey("replicationSource")) {
            builder.withReplicationSource(item.get("replicationSource").getS());
        }
        if (item.get("maxItems") != null) {
            builder.withMaxItems(Long.parseLong(item.get("maxItems").getN()));
        }
        if (item.containsKey("owner")) {
            builder.withOwner(item.get("owner").getS());
        }
        if (item.containsKey("storage")) {
            builder.withStorage(item.get("storage").getS());
        }
        if (item.containsKey("master")) {
            GlobalConfig global = new GlobalConfig();
            global.setMaster(item.get("master").getS());
            global.addSatellites(item.get("satellites").getSS());
            global.setIsMaster(item.get("isMaster").getBOOL());
            builder.withGlobal(global);
        }
        return builder.build();
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        List<ChannelConfig> configurations = new ArrayList<>();
        ScanRequest scanRequest = new ScanRequest()
                .withConsistentRead(true)
                .withTableName(getTableName());

        ScanResult result = dbClient.scan(scanRequest);
        mapItems(configurations, result);

        while (result.getLastEvaluatedKey() != null) {
            scanRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.scan(scanRequest);
            mapItems(configurations, result);
        }

        return configurations;
    }

    private void mapItems(List<ChannelConfig> configurations, ScanResult result) {
        for (Map<String, AttributeValue> item : result.getItems()) {
            configurations.add(mapItem(item));
        }
    }

    @Override
    public void delete(String name) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("key", new AttributeValue().withS(name));
        dbClient.deleteItem(new DeleteItemRequest(getTableName(), key));
    }

    private String getTableName() {
        return dynamoUtils.getTableName("channelMetaData");
    }

    private class DynamoChannelConfigurationDaoInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            initialize();
        }

        @Override
        protected void shutDown() throws Exception {
        }

    }
}
