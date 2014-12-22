package com.flightstats.hub.replication;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class DynamoReplicationDao implements ReplicationDao {
    private final static Logger logger = LoggerFactory.getLogger(DynamoReplicationDao.class);

    private final AmazonDynamoDBClient dbClient;
    private final DynamoUtils dynamoUtils;

    @Inject
    public DynamoReplicationDao(AmazonDynamoDBClient dbClient, DynamoUtils dynamoUtils) {
        this.dbClient = dbClient;
        this.dynamoUtils = dynamoUtils;
        HubServices.register(new DynamoReplicationDaoInit());
    }

    private class DynamoReplicationDaoInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            initialize();
        }

        @Override
        protected void shutDown() throws Exception { }

    }


    @Override
    public void upsert(ReplicationDomain domain) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("domain", new AttributeValue(domain.getDomain()));
        item.put("historicalDays", new AttributeValue().withN(String.valueOf(domain.getHistoricalDays())));
        if (!domain.getExcludeExcept().isEmpty()) {
            item.put("excludeExcept", new AttributeValue().withSS(domain.getExcludeExcept()));
        }
        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName())
                .withItem(item);
        dbClient.putItem(putItemRequest);
    }

    @Override
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

    @Override
    public void delete(String domain) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("domain", new AttributeValue().withS(domain));
        dbClient.deleteItem(new DeleteItemRequest(getTableName(), key));
    }

    @Override
    public Collection<ReplicationDomain> getDomains(boolean refreshCache) {
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

    private void initialize() {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(getTableName())
                .withAttributeDefinitions(new AttributeDefinition("domain", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("domain", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
        dynamoUtils.createTable(request);
    }

    private ReplicationDomain mapItem(Map<String, AttributeValue> item) {
        ReplicationDomain.ReplicationDomainBuilder builder = ReplicationDomain.builder()
                .domain(item.get("domain").getS());

        if (item.containsKey("historicalDays")) {
            builder.historicalDays(Long.parseLong(item.get("historicalDays").getN()));
        }
        if (item.containsKey("excludeExcept")) {
            builder.excludeExcept(new TreeSet<>(item.get("excludeExcept").getSS()));
        }
        return builder.build();
    }

    private String getTableName() {
        return dynamoUtils.getTableName("replicationConfig");
    }
}
