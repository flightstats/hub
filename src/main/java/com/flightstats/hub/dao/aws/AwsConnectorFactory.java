package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkBaseException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesCredentials;
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
import com.flightstats.hub.config.AwsProperty;
import com.flightstats.hub.config.DynamoProperty;
import com.flightstats.hub.config.S3Property;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.UnknownHostException;

import static com.amazonaws.retry.PredefinedBackoffStrategies.EqualJitterBackoffStrategy;
import static com.amazonaws.retry.PredefinedBackoffStrategies.FullJitterBackoffStrategy;

@Slf4j
public class AwsConnectorFactory {

    private AwsProperty awsProperty;
    private DynamoProperty dynamoProperty;
    private S3Property s3Property;

    private final String signingRegion;
    private final String protocol;

    @Inject
    public AwsConnectorFactory(AwsProperty awsProperty, DynamoProperty dynamoProperty, S3Property s3Property) {
        this.awsProperty = awsProperty;
        this.dynamoProperty = dynamoProperty;
        this.s3Property = s3Property;

        this.signingRegion = awsProperty.getSigningRegion();
        this.protocol = awsProperty.getProtocol();
    }

    public AmazonS3 getS3Client() {
        log.info("creating for  {} {} {}", protocol, s3Property.getEndpoint(), signingRegion);

        final ClientConfiguration clientConfiguration = getClientConfiguration(
                s3Property.getMaxConnections(),
                s3Property.getConnectionTimeout(),
                s3Property.getSocketTimeout(),
                true);

        log.info("using s3 config {}", clientConfiguration);

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withPathStyleAccessEnabled(s3Property.getPathStyleAccessEnable())
                .withChunkedEncodingDisabled(s3Property.getDisableChunkedEncoding())
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Property.getEndpoint(), signingRegion))
                .withCredentials(getAwsCredentials())
                .build();
    }

    public AmazonDynamoDB getDynamoClient() {
        log.info("creating for {} {} {}", protocol, dynamoProperty.getEndpoint(), signingRegion);

        final ClientConfiguration clientConfiguration = getClientConfiguration(
                dynamoProperty.getMaxConnections(),
                dynamoProperty.getConnectionTimeout(),
                dynamoProperty.getSocketTimeout(),
                false);

        log.info("using dynamo config {}", clientConfiguration);

        return AmazonDynamoDBClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dynamoProperty.getEndpoint(), signingRegion))
                .withCredentials(getAwsCredentials())
                .build();
    }

    private AWSCredentialsProviderChain getAwsCredentials() {
        return new AWSCredentialsProviderChain(
                new DefaultAWSCredentialsProviderChain(),
                new AWSStaticCredentialsProvider(loadTestCredentials(awsProperty.getCredentialsFile())));
    }

    private AWSCredentials loadTestCredentials(String credentialsPath) {
        log.info("loading test credentials " + credentialsPath);
        try {
            return new PropertiesCredentials(new File(credentialsPath));
        } catch (Exception e) {
            log.info("unable to load test credentials " + credentialsPath + " " + e.getMessage());
            return new BasicAWSCredentials("noKey", "noSecret");
        }
    }

    private ClientConfiguration getClientConfiguration(int maxConnections, int connectionTimeout, int socketTimeout, boolean compress) {
        RetryPolicy retryPolicy = new RetryPolicy(new HubRetryCondition(), new HubBackoffStrategy(awsProperty), 6, true);
        ClientConfiguration configuration = new ClientConfiguration()
                .withMaxConnections(maxConnections)
                .withRetryPolicy(retryPolicy)
                .withGzip(compress)
                .withProtocol(Protocol.valueOf(protocol))
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout);
        return configuration;
    }

    /**
     * Copied code from AWS client since SDKDefaultBackoffStrategy and V2CompatibleBackoffStrategyAdapter are not public.
     */
    static class HubBackoffStrategy implements RetryPolicy.BackoffStrategy {

        private final BackoffStrategy fullJitterBackoffStrategy;
        private final BackoffStrategy equalJitterBackoffStrategy;
        private int unknownHostDelay;

        HubBackoffStrategy(AwsProperty awsProperty) {
            this.unknownHostDelay = awsProperty.getRetryUnknownHostDelayInMillis();

            int delayMillis = awsProperty.getRetryDelayInMillis();
            int maxBackoffTime = awsProperty.getRetryMaxDelayInMillis();

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
            log.warn("exception {} from request {} attempts {}", exception, originalRequest, retriesAttempted);
            return retryCondition.shouldRetry(originalRequest, exception, retriesAttempted);
        }

    }

}
