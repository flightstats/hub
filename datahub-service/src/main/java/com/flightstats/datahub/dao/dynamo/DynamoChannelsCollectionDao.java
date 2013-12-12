package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.ChannelsCollectionDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class DynamoChannelsCollectionDao implements ChannelsCollectionDao {
    private final static Logger logger = LoggerFactory.getLogger(DynamoChannelsCollectionDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final String environment;
    private final TimeProvider timeProvider;
    private String tableName;

    @Inject
    public DynamoChannelsCollectionDao(AmazonDynamoDBClient dbClient,
                                       @Named("dynamo.environment") String environment,
                                       TimeProvider timeProvider) {

        this.dbClient = dbClient;
        this.environment = environment;
        this.timeProvider = timeProvider;
        tableName = "channelMetaData." + environment;
    }

    @Override
    public ChannelConfiguration createChannel(String name, Long ttlMillis) {
        ChannelConfiguration configuration = new ChannelConfiguration(name, timeProvider.getDate(), ttlMillis);
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
        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(tableName)
                .withItem(item);
        PutItemResult result = dbClient.putItem(putItemRequest);
    }

    @Override
    public void initializeMetadata() {
        try {
            logger.info(getDescribeTableResult().toString());
        } catch (ResourceNotFoundException e) {
            createTable();
        }

    }

    private DescribeTableResult getDescribeTableResult() {
        return dbClient.describeTable(tableName);
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
                .withTableName(tableName)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(ks)
                .withProvisionedThroughput(provisionedThroughput);
        CreateTableResult result = dbClient.createTable(request);
        logger.info(result.getTableDescription().toString());
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
                .withTableName(tableName)
                .withKey(keyMap);

        GetItemResult result = dbClient.getItem(getItemRequest);
        if (result.getItem() == null) {
            return null;
        }
        return mapItem(result.getItem());
    }

    private ChannelConfiguration mapItem(Map<String, AttributeValue> item) {
        Date date = new Date(Long.parseLong(item.get("date").getN()));
        AttributeValue millis = item.get("ttlMillis");
        Long ttlMillis = null;
        if (millis != null) {
            ttlMillis = Long.parseLong(millis.getN());
        }

        return new ChannelConfiguration(item.get("key").getS(), date, ttlMillis);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        //todo - gfm - 12/12/13 - this may need to use paging, if we have over 1MB worth of table names.
        List<ChannelConfiguration> configurations = new ArrayList<>();
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tableName);

        ScanResult result = dbClient.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()){
            configurations.add(mapItem(item));
        }
        return configurations;
    }

    @Override
    public boolean isHealthy() {
        try {
            getDescribeTableResult();
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
