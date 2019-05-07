package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.DynamoProperties;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class DynamoUtils {

    private final AmazonDynamoDB dbClient;
    private final AppProperties appProperties;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoUtils(AmazonDynamoDB dbClient,
                       AppProperties appProperties,
                       DynamoProperties dynamoProperties) {
        this.dbClient = dbClient;
        this.appProperties = appProperties;
        this.dynamoProperties = dynamoProperties;
    }

    String getLegacyTableName(String baseTableName) {
        return appProperties.getAppName() + "-" + appProperties.getEnv() + "-" + baseTableName;
    }


    void createAndUpdate(String tableName, String type, String keyName) {
        createAndUpdate(tableName, type, keyName, createTableRequest -> createTableRequest);
    }

    private void createAndUpdate(String tableName, String type, String keyName,
                                 Function<CreateTableRequest, CreateTableRequest> function) {
        ProvisionedThroughput throughput = getProvisionedThroughput(type);
        log.info("creating table {} ", tableName);
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
        long readThroughput = dynamoProperties.getThroughputRead(type);
        long writeThroughput = dynamoProperties.getThroughputWrite(type);
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
            log.info("Creating " + tableName + " ...");
            waitForTableStatus(tableName, TableStatus.ACTIVE);
        }
    }

    void updateTable(String tableName, ProvisionedThroughput throughput) {
        try {
            TableDescription tableDescription = waitForTableStatus(tableName, TableStatus.ACTIVE);
            ProvisionedThroughputDescription provisionedThroughput = tableDescription.getProvisionedThroughput();
            if (provisionedThroughput.getReadCapacityUnits().equals(throughput.getReadCapacityUnits())
                    && provisionedThroughput.getWriteCapacityUnits().equals(throughput.getWriteCapacityUnits())) {
                log.info("table is already at provisioned throughput {} {}", tableName, throughput);
            } else {
                log.info("updating table {} to {}", tableName, throughput);
                dbClient.updateTable(tableName, throughput);
                waitForTableStatus(tableName, TableStatus.ACTIVE);
            }
        } catch (ResourceNotFoundException e) {
            log.warn("update presumes the table exists " + tableName, e);
            throw new RuntimeException("unable to update table " + tableName);
        }
    }

    public boolean doesTableExist(String tableName) {
        return getTableDescription(tableName, TableStatus.ACTIVE).isPresent();
    }

    private TableDescription waitForTableStatus(String tableName, TableStatus status) {
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(dynamoProperties.getTableCreationWaitInMinutes());
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
