package com.flightstats.hub.dao.aws;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.retry.v2.BackoffStrategy;
import com.amazonaws.retry.v2.RetryPolicyContext;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
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
    private final String signingRegion = HubProperties.getSigningRegion();

    public AmazonS3 getS3Client() throws IOException {
        logger.info("creating for  " + protocol + " " + s3Endpoint + " " + signingRegion);
        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration("s3", true))
                .withPathStyleAccessEnabled(HubProperties.getProperty("s3.pathStyleAccessEnable", false))
                .withChunkedEncodingDisabled(HubProperties.getProperty("s3.disableChunkedEncoding", false))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, signingRegion))
                .withCredentials(getAwsCredentials())
                .build();
    }

    public AmazonDynamoDB getDynamoClient() throws IOException {
        logger.info("creating for  " + protocol + " " + dynamoEndpoint + " " + signingRegion);
        return AmazonDynamoDBClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration("dynamo", false))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dynamoEndpoint, signingRegion))
                .withCredentials(getAwsCredentials())
                .build();
    }

    private AWSCredentialsProviderChain getAwsCredentials() {
        return new AWSCredentialsProviderChain(
                new DefaultAWSCredentialsProviderChain(),
                new AWSStaticCredentialsProvider(loadTestCredentials(HubProperties.getProperty("aws.credentials", "hub_test_credentials.properties"))));
    }

    private AWSCredentials loadTestCredentials(String credentialsPath) {
        logger.info("loading test credentials " + credentialsPath);
        try {
            return new PropertiesCredentials(new File(credentialsPath));
        } catch (Exception e) {
            logger.info("unable to load test credentials " + credentialsPath + " " + e.getMessage());
            return new BasicAWSCredentials("noKey", "noSecret");
        }
    }

    private ClientConfiguration getClientConfiguration(String name, boolean compress) {
        RetryPolicy retryPolicy = new RetryPolicy(new HubRetryCondition(), new HubBackoffStrategy(), 6, true);
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

    static class HubRetryCondition implements RetryPolicy.RetryCondition {

        private final RetryPolicy.RetryCondition retryCondition = PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION;

        @Override
        public boolean shouldRetry(AmazonWebServiceRequest originalRequest, AmazonClientException exception, int retriesAttempted) {
            logger.warn("exception {} from request {} attempts {}", exception, originalRequest, retriesAttempted);
            return retryCondition.shouldRetry(originalRequest, exception, retriesAttempted);
        }

    }

}
