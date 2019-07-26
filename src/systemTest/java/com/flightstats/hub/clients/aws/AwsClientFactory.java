package com.flightstats.hub.clients.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;

import static com.flightstats.hub.system.config.PropertiesName.DYNAMODB_URL_TEMPLATE;
import static com.flightstats.hub.system.config.PropertiesName.S3_URL_TEMPLATE;

@Getter
@Slf4j
public class AwsClientFactory {
    private final String s3Endpoint;
    private final String dynamoEndpoint;
    private final AwsConfig config;

    @Inject
    public AwsClientFactory(@Named(S3_URL_TEMPLATE) String s3Endpoint,
                            @Named(DYNAMODB_URL_TEMPLATE) String dynamoEndpoint,
                            AwsConfig config) {
        this.s3Endpoint = s3Endpoint;
        this.dynamoEndpoint = dynamoEndpoint;
        this.config = config;
    }

    public AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(config.getEndpointConfiguration(s3Endpoint))
                .withCredentials(config.getAwsCredentials())
                .build();
    }

    public AmazonDynamoDB getDynamoDbClient() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(config.getEndpointConfiguration(dynamoEndpoint))
                .withCredentials(config.getAwsCredentials())
                .build();
    }
}
