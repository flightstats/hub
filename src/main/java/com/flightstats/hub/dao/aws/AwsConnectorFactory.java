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
    private final boolean useEncrypted;

    @Inject
	public AwsConnectorFactory(@Named("dynamo.endpoint") String endpoint,
                               @Named("aws.protocol") String protocol,
                               @Named("app.encrypted") boolean useEncrypted){
        this.endpoint = endpoint;
        this.protocol = protocol;
        this.useEncrypted = useEncrypted;
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
        //todo - gfm - 2/27/14 - pull this into config
        amazonS3Client.setEndpoint("s3-external-1.amazonaws.com");
        return amazonS3Client;
    }

    public AmazonDynamoDBClient getDynamoClient() throws IOException {
        logger.info("creating for  " + protocol + " " + endpoint);
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
        client.setEndpoint(endpoint);
        return client;

    }

    private PropertiesCredentials getPropertiesCredentials()  {
        if (useEncrypted) {
            return loadTestCredentials("/encrypted_test_credentials.properties");
        }
        return loadTestCredentials("/test_credentials.properties");
    }

    private PropertiesCredentials loadTestCredentials(String fileName) {
        logger.info("loading test credentials " + fileName);
        try {
            return new PropertiesCredentials(new File(
                    AwsConnectorFactory.class.getResource(fileName).getFile()));
        } catch (Exception e1) {
            logger.warn("unable to load test_credentials", e1);
            throw new RuntimeException(e1);
        }
    }

    private ClientConfiguration getClientConfiguration() {
        RetryPolicy retryPolicy = new RetryPolicy(new HubRetryCondition(),
                PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 3, true);
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.withRetryPolicy(retryPolicy);
        configuration.setProtocol(Protocol.valueOf(protocol));
        return configuration;
    }

}
