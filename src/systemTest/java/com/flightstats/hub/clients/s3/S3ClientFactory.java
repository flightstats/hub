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

@Getter
@Slf4j
public class S3ClientFactory {
    private final String releaseName;
    private final String s3Endpoint;
    private final String s3Region;
    private final String s3CredentialPath;
    private final boolean s3MockEnabled;

    @Inject
    public S3ClientFactory(@Named("helm.release.name") String releaseName,
                           @Named("s3.url") String s3Endpoint,
                           @Named("s3.region") String s3Region,
                           @Named("s3.credentials.path") String s3CredentialPath,
                           @Named("s3.mock.enabled") String s3MockProperty) {
        this.releaseName = releaseName;
        this.s3Endpoint = s3Endpoint;
        this.s3Region = s3Region;
        this.s3CredentialPath = s3CredentialPath;
        this.s3MockEnabled = Boolean.parseBoolean(s3MockProperty);
    }

    public AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(s3MockEnabled)
                .withChunkedEncodingDisabled(s3MockEnabled)
                .withEndpointConfiguration(new EndpointConfiguration(s3Endpoint, s3Region))
                .withCredentials(getAwsCredentials())
                .build();
    }

    private AWSCredentials loadTestCredentials() {
        try {
            return new PropertiesCredentials(new File(s3CredentialPath));
        } catch (Exception e) {
            log.error("error loading test credentials for s3, using dummies", e);
            return new BasicAWSCredentials("accessKey", "secretKey");
        }
    }

    private AWSCredentialsProviderChain getAwsCredentials() {
        return new AWSCredentialsProviderChain(new AWSStaticCredentialsProvider(loadTestCredentials()));
    }
}
