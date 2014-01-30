package com.flightstats.datahub.replication;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.datahub.dao.dynamo.DynamoUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class DynamoReplicationDao {
    private final static Logger logger = LoggerFactory.getLogger(DynamoReplicationDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoReplicationDao(AmazonDynamoDBClient dbClient, DynamoUtils dynamoUtils) {
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
    }

    public void upsert(ReplicationDomain config) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("domain", new AttributeValue(config.getDomain()));
        item.put("historicalDays", new AttributeValue().withN(String.valueOf(config.getHistoricalDays())));
        if (!config.getExcludeExcept().isEmpty()) {
            item.put("excludeExcept", new AttributeValue().withSS(config.getExcludeExcept()));
        }
        if (!config.getIncludeExcept().isEmpty()) {
            item.put("includeExcept", new AttributeValue().withSS(config.getIncludeExcept()));
        }

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName())
                .withItem(item);
        dbClient.putItem(putItemRequest);
    }

    public Optional<ReplicationDomain> get(String domain) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("domain", new AttributeValue().withS(domain));
        GetItemRequest getItemRequest = new GetItemRequest().withTableName(getTableName()).withKey(keyMap);
        try {
            GetItemResult result = dbClient.getItem(getItemRequest);
            if (result.getItem() == null) {
                return Optional.absent();
            }
            return Optional.of(mapItem(result.getItem()));
        } catch (ResourceNotFoundException e) {
            logger.info("config not found " + e.getMessage());
        }
        return Optional.absent();
    }

    public void delete(String domain) {
        //todo - gfm - 1/27/14 - delete
    }

    public Collection<ReplicationDomain> getDomains() {
        List<ReplicationDomain> domains = new ArrayList<>();
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(getTableName());

        ScanResult result = dbClient.scan(scanRequest);
        mapItems(domains, result);
        while (result.getLastEvaluatedKey() != null) {
            scanRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
            result = dbClient.scan(scanRequest);
            mapItems(domains, result);
        }
        return domains;
    }

    private void mapItems(List<ReplicationDomain> configurations, ScanResult result) {
        for (Map<String, AttributeValue> item : result.getItems()){
            configurations.add(mapItem(item));
        }
    }

    public void initialize() {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(getTableName())
                .withAttributeDefinitions(new AttributeDefinition("domain", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("domain", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
        dynamoUtils.createTable(request);
    }

    private ReplicationDomain mapItem(Map<String, AttributeValue> item) {
        ReplicationDomain.Builder builder = ReplicationDomain.builder()
                .withDomain(item.get("domain").getS());
        if (item.containsKey("historicalDays")) {
            builder.withHistoricalDays(Long.parseLong(item.get("historicalDays").getN()));
        }
        if (item.containsKey("excludeExcept")) {
            builder.withExcludedExcept(item.get("excludeExcept").getSS());
        }
        if (item.containsKey("includeExcept")) {
            builder.withIncludedExcept(item.get("includeExcept").getSS());
        }
        return builder.build();
    }

    public String getTableName() {
        return dynamoUtils.getTableName("replicationConfig");
    }
}
