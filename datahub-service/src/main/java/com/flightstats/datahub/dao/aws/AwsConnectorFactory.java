package com.flightstats.datahub.dao.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;

public class AwsConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(AwsConnectorFactory.class);

    private final String endpoint;
    private final String protocol;
    private final String credentials;

    @Inject
	public AwsConnectorFactory(@Named("dynamo.endpoint") String endpoint,
                               @Named("aws.protocol") String protocol,
                               @Named("aws.credentials") String credentials){
        this.endpoint = endpoint;
        this.protocol = protocol;
        this.credentials = credentials;
    }

    public AmazonS3 getS3Client() throws IOException {
        return new AmazonS3Client(getCredentials(), getClientConfiguration());
    }

    public AmazonDynamoDBClient getDynamoClient() throws IOException {
        logger.info("creating for  " + protocol + " " + endpoint);
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(getCredentials());
        ClientConfiguration configuration = getClientConfiguration();
        client.setConfiguration(configuration);
        client.setEndpoint(endpoint);
        return client;

    }

    private PropertiesCredentials getCredentials() throws IOException {
        //todo - gfm - 12/12/13 - figure out credentials
        //look at com.amazonaws.auth.AWSCredentialsProvider
        try {
            return new PropertiesCredentials(new File(credentials));
        } catch (IOException e) {
            try {
                return new PropertiesCredentials(new File(
                        AwsConnectorFactory.class.getResource("/test_credentials.properties").getFile()));
            } catch (Exception e1) {
                logger.warn("unable to load test_credentials", e1);
                throw e;
            }
        }
    }

    private ClientConfiguration getClientConfiguration() {
        RetryPolicy retryPolicy = new RetryPolicy(new DatahubRetryCondition(),
                PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 3, true);
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.withRetryPolicy(retryPolicy);
        configuration.setProtocol(Protocol.valueOf(protocol));
        return configuration;
    }

}
