package com.flightstats.hub.config;

import javax.inject.Inject;

public class S3Properties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public S3Properties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getEnv() {
        return propertiesLoader.getProperty("s3.environment", "local");
    }

    public String getEndpoint() {
        return propertiesLoader.getProperty("s3.endpoint", "s3-external-1.amazonaws.com");
    }

    public String getBucketName(String legacyS3BucketName) {
        return propertiesLoader.getProperty("s3.bucket_name", legacyS3BucketName);
    }

    public int getBucketPolicyMaxRules(int defaultValue) {
        return propertiesLoader.getProperty("s3.maxRules", defaultValue);
    }

    public boolean isBatchManagementEnabled() {
        return propertiesLoader.getProperty("s3.batch.management.enabled", true);
    }

    public boolean isConfigManagementEnabled() {
        return propertiesLoader.getProperty("s3.config.management.enabled", true);
    }

    public boolean isPathStyleAccessEnabled() {
        return propertiesLoader.getProperty("s3.pathStyleAccessEnable", false);
    }

    public boolean isChunkedEncodingEnabled() {
        return propertiesLoader.getProperty("s3.disableChunkedEncoding", false);
    }

    public int getWriteQueueSize() {
        return propertiesLoader.getProperty("s3.writeQueueSize", 40000);
    }

    public int getWriteQueueThreadCount() {
        return propertiesLoader.getProperty("s3.writeQueueThreads", 20);
    }

    public int getMaxConnections() {
        return propertiesLoader.getProperty("s3.maxConnections", 50);
    }

    public int getConnectionTimeout() {
        return propertiesLoader.getProperty("s3.connectionTimeout", 10 * 1000);
    }

    public int getSocketTimeout() {
        return propertiesLoader.getProperty("s3.socketTimeout", 30 * 1000);
    }

    public boolean isVerifierEnabled() {
        return propertiesLoader.getProperty("s3Verifier.run", true);
    }

    public int getMaxQueryItems() {
        return propertiesLoader.getProperty("s3.maxQueryItems", 1000);
    }

    public int getVerifierBaseTimeoutInMins() {
        return propertiesLoader.getProperty("s3Verifier.baseTimeoutMinutes", 2);
    }

    public int getVerifierOffsetInInMins() {
        return propertiesLoader.getProperty("s3Verifier.offsetMinutes", 15);
    }

    public int getVerifierChannelThreads() {
        return propertiesLoader.getProperty("s3Verifier.channelThreads", 3);
    }

    public boolean isChannelTtlEnforced() {
        return propertiesLoader.getProperty("channel.enforceTTL", false);
    }

    public int getLargeThreadCount() {
        return propertiesLoader.getProperty("s3.large.threads", 3);
    }

    public int getMaxChunkInMB() {
        return propertiesLoader.getProperty("s3.maxChunkMB", 40);
    }

}
