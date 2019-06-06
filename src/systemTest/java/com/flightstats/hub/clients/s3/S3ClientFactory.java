package com.flightstats.hub.clients.s3;

import static com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

import static com.flightstats.hub.system.config.PropertiesName.HELM_RELEASE_NAME;
import static com.flightstats.hub.system.config.PropertiesName.S3_CREDENTIALS_PATH;
import static com.flightstats.hub.system.config.PropertiesName.S3_URL_TEMPLATE;
import static com.flightstats.hub.system.config.PropertiesName.S3_REGION;

@Getter
@Slf4j
public class S3ClientFactory {
    private final String releaseName;
    private final String s3Endpoint;
    private final String s3Region;
    private final String s3CredentialPath;

    @Inject
    public S3ClientFactory(@Named(HELM_RELEASE_NAME) String releaseName,
                           @Named(S3_URL_TEMPLATE) String s3Endpoint,
                           @Named(S3_REGION) String s3Region,
                           @Named(S3_CREDENTIALS_PATH) String s3CredentialPath) {
        this.releaseName = releaseName;
        this.s3Endpoint = s3Endpoint;
        this.s3Region = s3Region;
        this.s3CredentialPath = s3CredentialPath;
    }

    public AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(String.format(s3Endpoint, releaseName), s3Region))
                .withCredentials(getAwsCredentials())
                .build();
    }

    private AWSCredentials loadTestCredentials() {
        try {
            return new PropertiesCredentials(new File(s3CredentialPath));
        } catch (Exception e) {
            log.info("could not load credentials for s3, using dummies. Reason: {}", e.getMessage());
            return new BasicAWSCredentials("accessKey", "secretKey");
        }
    }

    private AWSCredentialsProviderChain getAwsCredentials() {
        return new AWSCredentialsProviderChain(new AWSStaticCredentialsProvider(loadTestCredentials()));
    }
}
