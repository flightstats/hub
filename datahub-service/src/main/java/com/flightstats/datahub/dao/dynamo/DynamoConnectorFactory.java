package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.inject.Inject;
import com.google.inject.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public class DynamoConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(DynamoConnectorFactory.class);
	private static final int SECONDS_BETWEEN_CONNECTION_RETRIES = 5;

    private final String endpoint;
    private final String protocol;
    private final String credentials;

    @Inject
	public DynamoConnectorFactory(@Named("dynamo.endpoint") String endpoint,
                                  @Named("dynamo.protocol") String protocol,
                                  @Named("dynamo.credentials") String credentials){
        this.endpoint = endpoint;
        this.protocol = protocol;
        this.credentials = credentials;
    }

    @Provides
    public AmazonDynamoDBClient getClient() {
        while (true) {
            try {
                return attemptClient();
            } catch (Exception e) {
                //datastax driver doesn't use a common exception class to handle.
                logErrorAndWait(e);
            }
        }
    }

    private AmazonDynamoDBClient attemptClient() {
        try {
            logger.info("creating for  " + protocol + " " + endpoint);
            //todo - gfm - 12/12/13 - figure out credentials
            //look at com.amazonaws.auth.AWSCredentialsProvider
            AWSCredentials awsCredentials = new PropertiesCredentials(new File(credentials));
            AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentials);
            ClientConfiguration configuration = new ClientConfiguration();

            configuration.setProtocol(Protocol.valueOf(protocol));
            client.setConfiguration(configuration);
            client.setEndpoint(endpoint);
            return client;
        } catch (Exception e) {
            logger.error("unable to load credentials", e);
            throw new RuntimeException(e);
        }
    }

    private void logErrorAndWait(Exception e) {
		logger.error("Error creating AmazonDynamoDBClient: " + e.getMessage(), e);
		logger.info("Sleeping before retrying...");
		sleepUninterruptibly(SECONDS_BETWEEN_CONNECTION_RETRIES, TimeUnit.SECONDS);
		logger.info("Retrying AmazonDynamoDBClient connection");
	}


}
