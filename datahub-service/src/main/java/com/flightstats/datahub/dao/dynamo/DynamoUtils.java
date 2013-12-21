package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DynamoUtils {

    private final static Logger logger = LoggerFactory.getLogger(DynamoUtils.class);

    private final AmazonDynamoDBClient dbClient;
    private final String environment;
    private final int tableCreationWaitMinutes;

    @Inject
    public DynamoUtils(AmazonDynamoDBClient dbClient,
                       @Named("dynamo.environment") String environment,
                       @Named("dynamo.table.creation.wait.minutes") int tableCreationWaitMinutes) {
        this.dbClient = dbClient;
        this.environment = environment;
        this.tableCreationWaitMinutes = tableCreationWaitMinutes;
    }

    public String getTableName(String channelName) {
        return "deihub_" + environment + "_" + channelName;
    }

    /**
     * If a table does not already exist, create it.
     * Waits for the table to become ready for use.
     */
    public void createTable(CreateTableRequest request) {
        try {
            logger.info(dbClient.describeTable(request.getTableName()).toString());
        } catch (ResourceNotFoundException e) {
            dbClient.createTable(request);
            waitForTableToBecomeActive(request.getTableName());
        }
    }

    public void changeProvisioning(String channelName, ProvisionedThroughput provisionedThroughput) {
        //todo - gfm - 12/13/13 - this needs to consider relative percent change of provisioning as well as max changes per day
        String tableName = getTableName(channelName);
        DescribeTableResult describeTableResult = dbClient.describeTable(tableName);
        ProvisionedThroughputDescription existingThroughput = describeTableResult.getTable().getProvisionedThroughput();
        if (existingThroughput.getReadCapacityUnits().equals(provisionedThroughput.getReadCapacityUnits())
                && existingThroughput.getWriteCapacityUnits().equals(provisionedThroughput.getWriteCapacityUnits())) {
            logger.info("table " + tableName + " is already at this capacity ");
        }
        dbClient.updateTable(tableName, provisionedThroughput);
        waitForTableToBecomeActive(tableName);
    }

    //todo - gfm - 12/12/13 - make this generic for states for deletion - needs to wait for ResourceNotFoundException
    private void waitForTableToBecomeActive(String tableName) {
        logger.info("Waiting for " + tableName + " to become ACTIVE...");
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(tableCreationWaitMinutes);
        while (System.currentTimeMillis() < endTime) {
            try {
                DescribeTableRequest request = new DescribeTableRequest(tableName);
                TableDescription tableDescription = dbClient.describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                logger.debug("  - current state: " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                    logger.info("table " + tableName + " is active");
                    return;
                }
            } catch (AmazonServiceException ase) {
                logger.info("exception creating table " + tableName + " " + ase.getMessage());
                if (!ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException")) {
                    throw ase;
                }
                logger.info("retrying for table " + tableName);
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        logger.warn("table never went active " + tableName);
        throw new RuntimeException("Table " + tableName + " never went active");
    }
}
