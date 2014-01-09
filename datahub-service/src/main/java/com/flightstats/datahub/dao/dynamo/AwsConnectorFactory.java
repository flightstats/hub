package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.inject.Inject;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public class AwsConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(AwsConnectorFactory.class);
	private static final int SECONDS_BETWEEN_CONNECTION_RETRIES = 5;

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
        //todo - gfm - 1/3/14 - anything more needed here?
        AWSCredentials awsCredentials = new PropertiesCredentials(new File(credentials));
        ClientConfiguration configuration = getClientConfiguration();
        return new AmazonS3Client(awsCredentials, configuration);
    }

    public AmazonDynamoDBClient getDynamoClient() {
        while (true) {
            try {
                return attemptClient();
            } catch (Exception e) {
                logErrorAndWait(e);
            }
        }
    }

    private class ModifiedRetryCondition implements RetryPolicy.RetryCondition {

        @Override
        public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                                   AmazonClientException exception,
                                   int retriesAttempted) {
            // Always retry on client exceptions caused by IOException
            if (exception.getCause() instanceof IOException) return true;

            // Only retry on a subset of service exceptions
            if (exception instanceof AmazonServiceException) {
                AmazonServiceException ase = (AmazonServiceException)exception;

                /*
                 * For 500 internal server errors and 503 service
                 * unavailable errors, we want to retry, but we need to use
                 * an exponential back-off strategy so that we don't overload
                 * a server with a flood of retries.
                 */
                if (ase.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
                        || ase.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                    return true;
                }

                /*
                 * Throttling is reported as a 400 error from newer services. To try
                 * and smooth out an occasional throttling error, we'll pause and
                 * retry, hoping that the pause is long enough for the request to
                 * get through the next time.
                 */
                if (RetryUtils.isThrottlingException(ase)) {
                    if (PutItemRequest.class.isAssignableFrom(originalRequest.getClass())) {
                        PutItemRequest putItemRequest = (PutItemRequest) originalRequest;
                        logger.info("throttling put in table  " + putItemRequest.getTableName());
                    } else {
                        logger.info("throttling exception " + originalRequest);
                    }
                    return false;
                }

                /*
                 * Clock skew exception. If it is then we will get the time offset
                 * between the device time and the server time to set the clock skew
                 * and then retry the request.
                 */
                if (RetryUtils.isClockSkewError(ase)) return true;
            }

            return false;
        }
    }

    private AmazonDynamoDBClient attemptClient() {
        try {

            logger.info("creating for  " + protocol + " " + endpoint);
            //todo - gfm - 12/12/13 - figure out credentials
            //look at com.amazonaws.auth.AWSCredentialsProvider
            AWSCredentials awsCredentials = new PropertiesCredentials(new File(credentials));
            AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentials);
            ClientConfiguration configuration = getClientConfiguration();
            client.setConfiguration(configuration);
            client.setEndpoint(endpoint);
            return client;
        } catch (Exception e) {
            logger.error("unable to load credentials", e);
            throw new RuntimeException(e);
        }
    }

    private ClientConfiguration getClientConfiguration() {
        RetryPolicy retryPolicy = new RetryPolicy(new ModifiedRetryCondition(),
                PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 3, true);
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.withRetryPolicy(retryPolicy);
        configuration.setProtocol(Protocol.valueOf(protocol));
        return configuration;
    }

    private void logErrorAndWait(Exception e) {
		logger.error("Error creating AmazonDynamoDBClient: " + e.getMessage(), e);
		logger.info("Sleeping before retrying...");
		sleepUninterruptibly(SECONDS_BETWEEN_CONNECTION_RETRIES, TimeUnit.SECONDS);
		logger.info("Retrying AmazonDynamoDBClient connection");
	}


}
