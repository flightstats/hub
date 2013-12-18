package com.flightstats.datahub.dao.dynamo;

import com.flightstats.datahub.dao.ChannelDaoLocalIntegration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Properties;

public class DynamoChannelDaoLocalIntegration extends ChannelDaoLocalIntegration {

    @BeforeClass
    public static void setupClass() throws Exception {
        //todo - gfm - 12/12/13 - this requires DynamoDBLocal - http://aws.typepad.com/aws/2013/09/dynamodb-local-for-desktop-development.html
        //start with java -jar DynamoDBLocal.jar
        //todo - gfm - 12/12/13 - figure out how to run from IDE
        Properties properties = new Properties();

        properties.put("backing.store", "dynamo");
        //properties.put("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
        properties.put("dynamo.endpoint", "localhost:8000");
        properties.put("dynamo.protocol", "HTTP");
        properties.put("dynamo.environment", "dev");
        properties.put("dynamo.table.creation.wait.minutes", "5");
        //todo - gfm - 12/13/13 - make this generic
        properties.put("dynamo.credentials", "/Users/gmoulliet/code/datahub/datahub-service/src/conf/datahub/dev/credentials.properties");
        properties.put("hazelcast.conf.xml", "");
        finalStartup(properties);
    }

    @AfterClass
    public static void teardownClass() {
        //todo - gfm - 12/13/13 - delete any tables?

    }

    @Override
    protected void verifyStartup() {
    }
}
