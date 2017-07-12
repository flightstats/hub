package com.flightstats.hub.dao.aws;

import com.amazonaws.*;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.retry.v2.BackoffStrategy;
import com.amazonaws.retry.v2.RetryPolicyContext;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.flightstats.hub.app.HubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import static com.amazonaws.retry.PredefinedBackoffStrategies.EqualJitterBackoffStrategy;
import static com.amazonaws.retry.PredefinedBackoffStrategies.FullJitterBackoffStrategy;

public class AwsConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(AwsConnectorFactory.class);

    private final String dynamoEndpoint = HubProperties.getProperty("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
    private final String s3Endpoint = HubProperties.getProperty("s3.endpoint", "s3-external-1.amazonaws.com");
    private final String protocol = HubProperties.getProperty("aws.protocol", "HTTP");

    public AmazonS3 getS3Client() throws IOException {
        AmazonS3Client amazonS3Client;
        ClientConfiguration configuration = getClientConfiguration("s3", true);
        try {
            InstanceProfileCredentialsProvider credentialsProvider = new InstanceProfileCredentialsProvider();
            credentialsProvider.getCredentials();
            amazonS3Client = new AmazonS3Client(credentialsProvider, configuration);
        } catch (Exception e) {
            logger.warn("unable to use InstanceProfileCredentialsProvider " + e.getMessage());
            amazonS3Client = new AmazonS3Client(getPropertiesCredentials(), configuration);
        }
        amazonS3Client.setEndpoint(s3Endpoint);
        return amazonS3Client;
    }

    public AmazonDynamoDBClient getDynamoClient() throws IOException {
        logger.info("creating for  " + protocol + " " + dynamoEndpoint);
        AmazonDynamoDBClient client;
        ClientConfiguration configuration = getClientConfiguration("dynamo", false);
        try {
            InstanceProfileCredentialsProvider credentialsProvider = new InstanceProfileCredentialsProvider();
            credentialsProvider.getCredentials();
            client = new AmazonDynamoDBClient(credentialsProvider, configuration);
        } catch (Exception e) {
            logger.warn("unable to use InstanceProfileCredentialsProvider " + e.getMessage());
            client = new AmazonDynamoDBClient(getPropertiesCredentials(), configuration);
        }
        client.setEndpoint(dynamoEndpoint);
        return client;

    }

    private PropertiesCredentials getPropertiesCredentials() {
        return loadTestCredentials(HubProperties.getProperty("aws.credentials", "hub_test_credentials.properties"));
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

    private ClientConfiguration getClientConfiguration(String name, boolean compress) {
        RetryPolicy retryPolicy = new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                new HubBackoffStrategy(), 6, true);
        ClientConfiguration configuration = new ClientConfiguration()
                .withMaxConnections(HubProperties.getProperty(name + ".maxConnections", 50))
                .withRetryPolicy(retryPolicy)
                .withGzip(compress)
                .withProtocol(Protocol.valueOf(protocol))
                .withConnectionTimeout(HubProperties.getProperty(name + ".connectionTimeout", 10 * 1000))
                .withSocketTimeout(HubProperties.getProperty(name + ".socketTimeout", 30 * 1000));
        logger.info("using config {} {}", name, configuration);
        return configuration;
    }

    /**
     * Copied code from AWS client since SDKDefaultBackoffStrategy and V2CompatibleBackoffStrategyAdapter are not public.
     */
    static class HubBackoffStrategy implements RetryPolicy.BackoffStrategy {

        private final BackoffStrategy fullJitterBackoffStrategy;
        private final BackoffStrategy equalJitterBackoffStrategy;
        int unknownHostDelay = HubProperties.getProperty("aws.retry.unknown.host.delay.millis", 5000);

        HubBackoffStrategy() {
            int delayMillis = HubProperties.getProperty("aws.retry.delay.millis", 100);
            int maxBackoffTime = HubProperties.getProperty("aws.retry.max.delay.millis", 20 * 1000);
            fullJitterBackoffStrategy = new FullJitterBackoffStrategy(delayMillis, maxBackoffTime);
            equalJitterBackoffStrategy = new EqualJitterBackoffStrategy(delayMillis * 10, maxBackoffTime);  // bc doubling base delay
        }

        long computeDelayBeforeNextRetry(RetryPolicyContext context) {
            SdkBaseException exception = context.exception();
            if (RetryUtils.isThrottlingException(exception)) {
                return equalJitterBackoffStrategy.computeDelayBeforeNextRetry(context);
            } else if (exception != null && exception.getCause() != null
                    && exception.getCause() instanceof UnknownHostException) {
                return unknownHostDelay;
            } else {
                return fullJitterBackoffStrategy.computeDelayBeforeNextRetry(context);
            }
        }

        @Override
        public long delayBeforeNextRetry(AmazonWebServiceRequest originalRequest, AmazonClientException exception, int retriesAttempted) {
            return computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                    .originalRequest(originalRequest)
                    .exception(exception)
                    .retriesAttempted(retriesAttempted)
                    .build());
        }
    }


}
