package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.google.inject.Injector;

import java.util.Properties;

/**
 *
 */
public class DynamoTablesTool {
    public static void main(String[] args) throws ConstraintException {
        Properties properties = new Properties();

        properties.put("backing.store", "aws");
        properties.put("aws.protocol", "HTTP");
        properties.put("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
        properties.put("dynamo.environment", "dev");
        properties.put("dynamo.table.creation.wait.minutes", "10");
        properties.put("aws.credentials", "/Users/gmoulliet/code/datahub/datahub-service/src/conf/datahub/dev/credentials.properties");

        properties.put("hazelcast.conf.xml", "");
        Injector injector = GuiceContextListenerFactory.construct(properties).getInjector();
        AmazonDynamoDBClient dbClient = injector.getInstance(AmazonDynamoDBClient.class);
        DynamoUtils dynamoUtils = injector.getInstance(DynamoUtils.class);

        for (int i = 70; i <= 84; i++) {
            String channelName = "test" + i;
            //deleteChannel(dbClient, name);
            ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                    .withReadCapacityUnits(20L)
                    .withWriteCapacityUnits(20L);
            //dynamoUtils.changeProvisioning(channelName, provisionedThroughput);
        }

    }

    private static void deleteTable(AmazonDynamoDBClient dbClient, String name) {
        try {
            DeleteTableResult result = dbClient.deleteTable(name);
            System.out.println(result.getTableDescription().toString());
        } catch (AmazonClientException e) {
            System.out.println(e.getMessage());
        }
    }
}
