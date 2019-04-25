package com.flightstats.hub.config;

import javax.inject.Inject;

public class S3Property {

    private PropertyLoader propertyLoader;

    @Inject
    public S3Property(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getEnv() {
        return this.propertyLoader.getProperty("s3.environment", "local");
    }

    public String getEndpoint() {
        return this.propertyLoader.getProperty("s3.endpoint", "s3-external-1.amazonaws.com");
    }

    public String getBucketName(String legacyS3BucketName) {
        return this.propertyLoader.getProperty("s3.bucket_name", legacyS3BucketName);
    }

    public int getBucketPolicyMaxRules(int defaultValue) {
        return this.propertyLoader.getProperty("s3.maxRules", defaultValue);
    }

    public boolean getPathStyleAccessEnable() {
        return this.propertyLoader.getProperty("s3.pathStyleAccessEnable", false);
    }

    public boolean getDisableChunkedEncoding() {
        return this.propertyLoader.getProperty("s3.disableChunkedEncoding", false);
    }

    public int getWriteQueueSize() {
        return this.propertyLoader.getProperty("s3.writeQueueSize", 40000);
    }

    public int getWriteQueueThreadCount() {
        return this.propertyLoader.getProperty("s3.writeQueueThreads", 20);
    }

    public int getMaxConnections() {
        return this.propertyLoader.getProperty("s3.maxConnections", 50);
    }

    public int getConnectionTimeout() {
        return this.propertyLoader.getProperty("s3.connectionTimeout", 10 * 1000);
    }

    public int getSocketTimeout() {
        return this.propertyLoader.getProperty("s3.socketTimeout", 30 * 1000);
    }

    public boolean getVerifierRun() {
        return this.propertyLoader.getProperty("s3Verifier.run", true);
    }

    public int getMaxQueryItems() {
        return this.propertyLoader.getProperty("s3.maxQueryItems", 1000);
    }

    public int getVerifierBaseTimeoutInMins() {
        return this.propertyLoader.getProperty("s3Verifier.baseTimeoutMinutes", 2);
    }

    public int getVerifierOffsetInInMins() {
        return this.propertyLoader.getProperty("s3Verifier.offsetMinutes", 15);
    }

    public int getVerifierChannelThreads() {
        return this.propertyLoader.getProperty("s3Verifier.channelThreads", 3);
    }

    public boolean isChannelEnforceTTL() {
        return this.propertyLoader.getProperty("channel.enforceTTL", false);
    }

    public int getLargeThreadCount() {
        return this.propertyLoader.getProperty("s3.large.threads", 3);
    }

    public int getMaxChunkInMB() {
        return this.propertyLoader.getProperty("s3.maxChunkMB", 40);
    }

}
