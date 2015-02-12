package com.flightstats.hub.dao.dynamo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class DynamoUtils {

    private final static Logger logger = LoggerFactory.getLogger(DynamoUtils.class);

    private final AmazonDynamoDBClient dbClient;
    private final String environment;
    private final String appName;
    private final int tableCreationWaitMinutes;

    @Inject
    public DynamoUtils(AmazonDynamoDBClient dbClient,
                       @Named("app.environment") String environment, @Named("app.name") String appName,
                       @Named("dynamo.table_creation_wait_minutes") int tableCreationWaitMinutes) {
        this.dbClient = dbClient;
        this.environment = environment;
        this.appName = appName;
        this.tableCreationWaitMinutes = tableCreationWaitMinutes;
    }

    public String getTableName(String baseTableName) {
        return appName + "-" + environment + "-" + baseTableName;
    }

    /**
     * If a table does not already exist, create it.
     * Waits for the table to become ready for use.
     */
    public void createTable(CreateTableRequest request) {
        String tableName = request.getTableName();
        try {
            waitForTableStatus(tableName, TableStatus.ACTIVE);
        } catch (ResourceNotFoundException e) {
            dbClient.createTable(request);
            logger.info("Creating " + tableName + " ...");
            waitForTableStatus(tableName, TableStatus.ACTIVE);
        }
    }

    public void deleteChannel(String channelName) {
        String tableName = getTableName(channelName);
        try {
            DescribeTableResult describeTableResult = dbClient.describeTable(tableName);
            dbClient.deleteTable(tableName);
        } catch (ResourceNotFoundException e) {
            //do nothing
        }
    }

    private void waitForTableStatus(String tableName, TableStatus status) {
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(tableCreationWaitMinutes);
        while (System.currentTimeMillis() < endTime) {
            try {
                TableDescription tableDescription = dbClient.describeTable(tableName).getTable();
                if (status.equals(TableStatus.fromValue(tableDescription.getTableStatus()))) {
                    logger.info("table " + tableName + " is " + status.toString());
                    return;
                }
            } catch (AmazonServiceException ase) {
                logger.info("exception creating table " + tableName + " " + ase.getMessage());
                throw ase;
            }
            sleep();
        }
        logger.warn("table never went active " + tableName);
        throw new RuntimeException("Table " + tableName + " never went active");
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            //ignore
        }
    }
}
