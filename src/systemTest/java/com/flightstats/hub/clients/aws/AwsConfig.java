package com.flightstats.hub.clients.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

import static com.flightstats.hub.system.config.PropertiesName.AWS_REGION;
import static com.flightstats.hub.system.config.PropertiesName.HELM_RELEASE_NAME;
import static com.flightstats.hub.system.config.PropertiesName.S3_CREDENTIALS_PATH;

@Slf4j
class AwsConfig {
    private final String releaseName;
    private final String credentialPath;
    private final String awsRegion;

    @Inject
    AwsConfig(@Named(HELM_RELEASE_NAME) String releaseName,
              @Named(S3_CREDENTIALS_PATH) String credentialPath,
              @Named(AWS_REGION) String awsRegion) {
        this.releaseName = releaseName;
        this.credentialPath = credentialPath;
        this.awsRegion = awsRegion;
    }

    AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(String endpoint) {
        return new AwsClientBuilder.EndpointConfiguration(String.format(endpoint, releaseName), awsRegion);
    }

    AWSCredentialsProviderChain getAwsCredentials() {
        return new AWSCredentialsProviderChain(new DefaultAWSCredentialsProviderChain(), new AWSStaticCredentialsProvider(getTestCredentials()));
    }

    private AWSCredentials getTestCredentials() {
        try {
            return new PropertiesCredentials(new File(credentialPath));
        } catch (Exception e) {
            log.warn("could not load credentials for s3, using dummies. Reason: {}", e.getMessage());
            return new BasicAWSCredentials("accessKey", "secretKey");
        }
    }

}
