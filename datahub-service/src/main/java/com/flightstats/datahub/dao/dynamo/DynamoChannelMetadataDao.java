package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.ChannelMetadataDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class DynamoChannelMetadataDao implements ChannelMetadataDao {
    private final static Logger logger = LoggerFactory.getLogger(DynamoChannelMetadataDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoChannelMetadataDao(AmazonDynamoDBClient dbClient,
                                    DynamoUtils dynamoUtils) {

        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        updateChannel(configuration);
        return configuration;
    }

    @Override
    public void updateChannel(ChannelConfiguration config) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue().withS(config.getName()));
        item.put("date", new AttributeValue().withN(String.valueOf(config.getCreationDate().getTime())));
        if (config.getTtlMillis() != null) {
            item.put("ttlMillis", new AttributeValue().withN(String.valueOf(config.getTtlMillis())));
        }
        item.put("type", new AttributeValue().withS(config.getType().toString()));
        //todo - gfm - 12/23/13 - add size & rate info
        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName())
                .withItem(item);
        PutItemResult result = dbClient.putItem(putItemRequest);
    }

    @Override
    public void initializeMetadata() {
        createTable();
    }

    private void createTable() {
        ArrayList<AttributeDefinition> attributeDefinitions= new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("key").withAttributeType("S"));

        ArrayList<KeySchemaElement> ks = new ArrayList<>();
        ks.add(new KeySchemaElement().withAttributeName("key").withKeyType(KeyType.HASH));

        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(10L)
                .withWriteCapacityUnits(10L);

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(getTableName())
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(ks)
                .withProvisionedThroughput(provisionedThroughput);
        //todo - gfm - 12/23/13 - this needs to wait until the table is active, even if it already exists
        dynamoUtils.createTable(request);
    }

    @Override
    public boolean channelExists(String channelName) {
        return getChannelConfiguration(channelName) != null;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("key", new AttributeValue().withS(channelName));

        GetItemRequest getItemRequest = new GetItemRequest()
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

    private ChannelConfiguration mapItem(Map<String, AttributeValue> item) {
        //todo - gfm - 12/23/13 - add size & rate info
        Date date = new Date(Long.parseLong(item.get("date").getN()));
        AttributeValue millis = item.get("ttlMillis");
        Long ttlMillis = null;
        if (millis != null) {
            ttlMillis = Long.parseLong(millis.getN());
        }
        return ChannelConfiguration.builder()
                .withCreationDate(date)
                .withTtlMillis(ttlMillis)
                .withName(item.get("key").getS())
                .withType(ChannelConfiguration.ChannelType.valueOf(item.get("type").getS()))
                .build();

    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        //todo - gfm - 12/12/13 - this may need to use paging, if we have over 1MB worth of table configs.
        List<ChannelConfiguration> configurations = new ArrayList<>();
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(getTableName());

        ScanResult result = dbClient.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()){
            configurations.add(mapItem(item));
        }
        return configurations;
    }

    @Override
    public void delete(String channelName) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("key", new AttributeValue().withS(channelName));
        dbClient.deleteItem(new DeleteItemRequest(getTableName(), key));
    }

    @Override
    public boolean isHealthy() {
        try {
            dbClient.describeTable(getTableName());
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public String getTableName() {
        return dynamoUtils.getTableName("channelMetaData");
    }
}
