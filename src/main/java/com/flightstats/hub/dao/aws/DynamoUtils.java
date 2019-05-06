package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DynamoUtils {

    private final static Logger logger = LoggerFactory.getLogger(DynamoUtils.class);

    private final AmazonDynamoDB dbClient;
    private final String environment;
    private final String appName;
    private final int tableCreationWaitMinutes;

    @Inject
    public DynamoUtils(AmazonDynamoDB dbClient,
                       @Named("app.environment") String environment, @Named("app.name") String appName) {
        this.dbClient = dbClient;
        this.environment = environment;
        this.appName = appName;
        this.tableCreationWaitMinutes = HubProperties.getProperty("dynamo.table_creation_wait_minutes", 10);
    }

    String getLegacyTableName(String baseTableName) {
        return appName + "-" + environment + "-" + baseTableName;
    }

    void createAndUpdate(String tableName, String type, String keyName) {
        createAndUpdate(tableName, type, keyName, createTableRequest -> createTableRequest);
    }

    private void createAndUpdate(String tableName, String type, String keyName,
                                 Function<CreateTableRequest, CreateTableRequest> function) {
        ProvisionedThroughput throughput = getProvisionedThroughput(type);
        logger.info("creating table {} ", tableName);
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(new AttributeDefinition(keyName, ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement(keyName, KeyType.HASH))
                .withProvisionedThroughput(throughput);
        request = function.apply(request);
        createTable(request);
        updateTable(tableName, throughput);
    }

    ProvisionedThroughput getProvisionedThroughput(String type) {
        long readThroughput = HubProperties.getProperty("dynamo.throughput." + type + ".read", 100);
        long writeThroughput = HubProperties.getProperty("dynamo.throughput." + type + ".write", 10);
        return new ProvisionedThroughput(readThroughput, writeThroughput);
    }

    /**
     * If a table does not already exist, create it.
     * Waits for the table to become ready for use.
     */
    void createTable(CreateTableRequest request) {
        String tableName = request.getTableName();
        try {
            waitForTableStatus(tableName, TableStatus.ACTIVE);
        } catch (ResourceNotFoundException e) {
            dbClient.createTable(request);
            logger.info("Creating " + tableName + " ...");
            waitForTableStatus(tableName, TableStatus.ACTIVE);
        }
    }

    void updateTable(String tableName, ProvisionedThroughput throughput) {
        try {
            TableDescription tableDescription = waitForTableStatus(tableName, TableStatus.ACTIVE);
            ProvisionedThroughputDescription provisionedThroughput = tableDescription.getProvisionedThroughput();
            if (provisionedThroughput.getReadCapacityUnits().equals(throughput.getReadCapacityUnits())
                    && provisionedThroughput.getWriteCapacityUnits().equals(throughput.getWriteCapacityUnits())) {
                logger.info("table is already at provisioned throughput {} {}", tableName, throughput);
            } else {
                logger.info("updating table {} to {}", tableName, throughput);
                dbClient.updateTable(tableName, throughput);
                waitForTableStatus(tableName, TableStatus.ACTIVE);
            }
        } catch (ResourceNotFoundException e) {
            logger.warn("update presumes the table exists " + tableName, e);
            throw new RuntimeException("unable to update table " + tableName);
        }
    }

    public boolean doesTableExist(String tableName) {
        return getTableDescription(tableName, TableStatus.ACTIVE).isPresent();
    }

    private TableDescription waitForTableStatus(String tableName, TableStatus status) {
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(tableCreationWaitMinutes);
        while (System.currentTimeMillis() < endTime) {
            Optional<TableDescription> existing = getTableDescription(tableName, status);
            if (existing.isPresent()) {
                return existing.get();
            }
            sleep();
        }
        logger.warn("table never went active " + tableName);
        throw new RuntimeException("Table " + tableName + " never went active");
    }

    private Optional<TableDescription> getTableDescription(String tableName, TableStatus status) {
        try {
            TableDescription tableDescription = dbClient.describeTable(tableName).getTable();
            if (status == TableStatus.fromValue(tableDescription.getTableStatus())) {
                logger.info("table " + tableName + " is " + status.toString());
                return Optional.of(tableDescription);
            }
        } catch (AmazonServiceException ase) {
            logger.info("exception creating table " + tableName + " " + ase.getMessage());
            throw ase;
        }
        return Optional.empty();
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            //ignore
        }
    }
}
