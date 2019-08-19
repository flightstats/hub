package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.flightstats.hub.config.properties.DynamoProperties;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
public class DynamoUtils {

    private final AmazonDynamoDB dbClient;

    @Inject
    public DynamoUtils(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
   }

    boolean doesTableExist(String tableName) {
        return getTableDescription(tableName, TableStatus.ACTIVE).isPresent();
    }

    private Optional<TableDescription> getTableDescription(String tableName, TableStatus status) {
        try {
            TableDescription tableDescription = dbClient.describeTable(tableName).getTable();
            if (status == TableStatus.fromValue(tableDescription.getTableStatus())) {
                log.debug("table {} is {}", tableName, status.toString());
                return Optional.of(tableDescription);
            }
        } catch (AmazonServiceException ase) {
            log.error("exception creating table {}", tableName, ase);
            throw ase;
        }
        return Optional.empty();
    }
}
