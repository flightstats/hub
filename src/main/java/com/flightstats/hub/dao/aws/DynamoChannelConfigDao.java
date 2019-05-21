package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class DynamoChannelConfigDao implements Dao<ChannelConfig> {

    private final AmazonDynamoDB dbClient;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoChannelConfigDao(AmazonDynamoDB dbClient,
                                  DynamoProperties dynamoProperties) {
        this.dbClient = dbClient;
        this.dynamoProperties = dynamoProperties;
    }

    @Override
    public void upsert(ChannelConfig config) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", new AttributeValue(config.getName().toLowerCase()));
        item.put("displayName", new AttributeValue(config.getDisplayName()));
        item.put("date", new AttributeValue().withN(String.valueOf(config.getCreationDate().getTime())));
        item.put("keepForever", new AttributeValue().withBOOL(config.getKeepForever()));
        item.put("ttlDays", new AttributeValue().withN(String.valueOf(config.getTtlDays())));
        item.put("maxItems", new AttributeValue().withN(String.valueOf(config.getMaxItems())));
        if (config.getMutableTime() != null) {
            item.put("mutableTime", new AttributeValue().withN(String.valueOf(config.getMutableTime().getMillis())));
        }
        item.put("protect", new AttributeValue().withBOOL(config.isProtect()));
        item.put("allowZeroBytes", new AttributeValue().withBOOL(config.isAllowZeroBytes()));
        item.put("secondaryMetricsReporting", new AttributeValue().withBOOL(config.isSecondaryMetricsReporting()));
        if (!config.getTags().isEmpty()) {
            item.put("tags", new AttributeValue().withSS(config.getTags()));
        }
        if (StringUtils.isNotEmpty(config.getDescription())) {
            item.put("description", new AttributeValue(config.getDescription()));
        }
        if (StringUtils.isNotEmpty(config.getReplicationSource())) {
            item.put("replicationSource", new AttributeValue(config.getReplicationSource()));
        }
        if (StringUtils.isNotEmpty(config.getOwner())) {
            item.put("owner", new AttributeValue(config.getOwner()));
        }
        if (StringUtils.isNotEmpty(config.getStorage())) {
            item.put("storage", new AttributeValue(config.getStorage()));
        }
        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(dynamoProperties.getChannelConfigTableName())
                .withItem(item);
        dbClient.putItem(putItemRequest);
    }

    @Override
    public ChannelConfig get(String name) {
        HashMap<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("key", new AttributeValue().withS(name));
        GetItemRequest getItemRequest = new GetItemRequest()
                .withConsistentRead(true)
                .withTableName(dynamoProperties.getChannelConfigTableName())
                .withKey(keyMap);
        try {
            GetItemResult result = dbClient.getItem(getItemRequest);
            if (result.getItem() == null) {
                return null;
            }
            return mapItem(result.getItem())
                    .orElseThrow(() -> new ResourceNotFoundException("Unable to read channel config from dynamo " + name));
        } catch (ResourceNotFoundException e) {
            log.info("channel not found " + e.getMessage());
            return null;
        }
    }

    @VisibleForTesting
    Optional<ChannelConfig> mapItem(Map<String, AttributeValue> item) {
        ChannelConfig cfg = null;
        try {
            ChannelConfig.ChannelConfigBuilder builder = ChannelConfig.builder()
                    .creationDate(new Date(Long.parseLong(item.get("date").getN())))
                    .name(item.get("key").getS())
                    .displayName(item.get("displayName").getS());
            if (item.get("ttlDays") != null) {
                builder.ttlDays(Long.parseLong(item.get("ttlDays").getN()));
            }
            if (item.get("keepForever") != null) {
                builder.keepForever(item.get("keepForever").getBOOL());
            }
            if (item.containsKey("description")) {
                builder.description(item.get("description").getS());
            }
            if (item.containsKey("tags")) {
                builder.tags(item.get("tags").getSS());
            }
            if (item.containsKey("replicationSource")) {
                builder.replicationSource(item.get("replicationSource").getS());
            }
            if (item.get("maxItems") != null) {
                builder.maxItems(Long.parseLong(item.get("maxItems").getN()));
            }
            if (item.containsKey("owner")) {
                builder.owner(item.get("owner").getS());
            }
            if (item.containsKey("storage")) {
                builder.storage(item.get("storage").getS());
            }
            if (item.containsKey("protect")) {
                builder.protect(item.get("protect").getBOOL());
            }
            if (item.containsKey("allowZeroBytes")) {
                builder.allowZeroBytes(item.get("allowZeroBytes").getBOOL());
            }
            if (item.containsKey("secondaryMetricsReporting")) {
                builder.secondaryMetricsReporting(item.get("secondaryMetricsReporting").getBOOL());
            }
            if (item.containsKey("mutableTime")) {
                builder.mutableTime(new DateTime(Long.parseLong(item.get("mutableTime").getN()), DateTimeZone.UTC));
            }
            cfg = builder.build();
        } catch (Exception e) {
            log.warn("Unable to map channel {} {}", item.get("key"), e.getMessage());
        }
        return Optional.ofNullable(cfg);
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        List<ChannelConfig> configurations = new ArrayList<>();
        ScanRequest scanRequest = new ScanRequest()
                .withConsistentRead(true)
                .withTableName(dynamoProperties.getChannelConfigTableName());

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
        result.getItems().forEach(
                item -> mapItem(item).ifPresent(configurations::add)
        );
    }

    @Override
    public void delete(String name) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("key", new AttributeValue().withS(name));
        dbClient.deleteItem(new DeleteItemRequest(dynamoProperties.getChannelConfigTableName(), key));
    }

}