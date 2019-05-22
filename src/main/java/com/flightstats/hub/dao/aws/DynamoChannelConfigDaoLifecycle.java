package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DynamoChannelConfigDaoLifecycle extends AbstractIdleService {
    private final DynamoUtils dynamoUtils;
    private final AppProperties appProperties;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoChannelConfigDaoLifecycle(DynamoUtils dynamoUtils,
                                           AppProperties appProperties,
                                           DynamoProperties dynamoProperties) {
        this.dynamoUtils = dynamoUtils;
        this.appProperties = appProperties;
        this.dynamoProperties = dynamoProperties;
    }

    @Override
    protected void startUp() {
        initialize();
    }

    @Override
    protected void shutDown() {
    }

    void initialize() {
        String tableName = dynamoProperties.getChannelConfigTableName();
        ProvisionedThroughput throughput = dynamoUtils.getProvisionedThroughput("channel");

        if (appProperties.isReadOnly()) {
            if (!dynamoUtils.doesTableExist(tableName)) {
                String msg = String.format("Probably fatal error. Dynamo channel config table doesn't exist for r/o node.  %s", tableName);
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } else {
            if (!dynamoUtils.doesTableExist(tableName)) {
                createTable(tableName, throughput);
            } else {
                dynamoUtils.updateTable(tableName, throughput);
            }
        }
    }

    private void createTable(String tableName, ProvisionedThroughput throughput) {
        log.info("creating table {} ", tableName);
        List<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(new AttributeDefinition("key", ScalarAttributeType.S));
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(new KeySchemaElement("key", KeyType.HASH))
                .withProvisionedThroughput(throughput);

        dynamoUtils.createTable(request);
    }

}