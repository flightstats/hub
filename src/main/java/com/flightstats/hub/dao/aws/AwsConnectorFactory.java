package com.flightstats.hub.dao.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class AwsConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(AwsConnectorFactory.class);

    private final String dynamoEndpoint;
    private final String s3Endpoint;
    private final String protocol;

    @Inject
    public AwsConnectorFactory() {
        this.dynamoEndpoint = HubProperties.getProperty("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
        this.s3Endpoint = HubProperties.getProperty("s3.endpoint", "s3-external-1.amazonaws.com");
        this.protocol = HubProperties.getProperty("aws.protocol", "HTTP");
        ;
    }

    public AmazonS3 getS3Client() throws IOException {
        AmazonS3Client amazonS3Client = null;
        try {
            InstanceProfileCredentialsProvider credentialsProvider = new InstanceProfileCredentialsProvider();
            credentialsProvider.getCredentials();
            amazonS3Client = new AmazonS3Client(credentialsProvider, getClientConfiguration());
        } catch (Exception e) {
            logger.warn("unable to use InstanceProfileCredentialsProvider " + e.getMessage());
            amazonS3Client = new AmazonS3Client(getPropertiesCredentials(), getClientConfiguration());
        }
        amazonS3Client.setEndpoint(s3Endpoint);
        return amazonS3Client;
    }

    public AmazonDynamoDBClient getDynamoClient() throws IOException {
        logger.info("creating for  " + protocol + " " + dynamoEndpoint);
        AmazonDynamoDBClient client = null;
        try {
            InstanceProfileCredentialsProvider credentialsProvider = new InstanceProfileCredentialsProvider();
            credentialsProvider.getCredentials();
            client = new AmazonDynamoDBClient(credentialsProvider);
        } catch (Exception e) {
            logger.warn("unable to use InstanceProfileCredentialsProvider " + e.getMessage());
            client = new AmazonDynamoDBClient(getPropertiesCredentials());
        }
        ClientConfiguration configuration = getClientConfiguration();
        client.setConfiguration(configuration);
        client.setEndpoint(dynamoEndpoint);
        return client;

    }

    private PropertiesCredentials getPropertiesCredentials() {
        return loadTestCredentials(HubProperties.getProperty("test_credentials", "hub_test_credentials.properties"));
    }

    private PropertiesCredentials loadTestCredentials(String credentialsPath) {
        logger.info("loading test credentials " + credentialsPath);
        try {
            return new PropertiesCredentials(new File(credentialsPath));
        } catch (Exception e1) {
            logger.warn("unable to load test credentials " + credentialsPath, e1);
            throw new RuntimeException(e1);
        }
    }

    private ClientConfiguration getClientConfiguration() {
        RetryPolicy retryPolicy = PredefinedRetryPolicies.getDefaultRetryPolicy();
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.withRetryPolicy(retryPolicy);
        configuration.setProtocol(Protocol.valueOf(protocol));
        return configuration;
    }

}
