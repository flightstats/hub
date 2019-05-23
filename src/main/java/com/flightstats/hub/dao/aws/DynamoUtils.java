package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.flightstats.hub.config.DynamoProperties;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
public class DynamoUtils {

    private final AmazonDynamoDB dbClient;
    private final DynamoProperties dynamoProperties;

    @Inject
    public DynamoUtils(AmazonDynamoDB dbClient,
                       DynamoProperties dynamoProperties) {
        this.dbClient = dbClient;
        this.dynamoProperties = dynamoProperties;
    }

    ProvisionedThroughput getProvisionedThroughput(String type) {
        long readThroughput = dynamoProperties.getThroughputRead(type);
        long writeThroughput = dynamoProperties.getThroughputWrite(type);
        return new ProvisionedThroughput(readThroughput, writeThroughput);
    }

    public boolean doesTableExist(String tableName) {
        return getTableDescription(tableName, TableStatus.ACTIVE).isPresent();
    }

    private Optional<TableDescription> getTableDescription(String tableName, TableStatus status) {
        try {
            TableDescription tableDescription = dbClient.describeTable(tableName).getTable();
            if (status == TableStatus.fromValue(tableDescription.getTableStatus())) {
                log.info("table " + tableName + " is " + status.toString());
                return Optional.of(tableDescription);
            }
        } catch (AmazonServiceException ase) {
            log.info("exception creating table " + tableName + " " + ase.getMessage());
            throw ase;
        }
        return Optional.empty();
    }
}
