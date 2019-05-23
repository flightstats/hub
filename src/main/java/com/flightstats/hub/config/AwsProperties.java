package com.flightstats.hub.config;

import javax.inject.Inject;

public class AwsProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public AwsProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getCredentialsFile() {
        return propertiesLoader.getProperty("aws.credentials", "hub_test_credentials.properties");
    }

    public String getProtocol() {
        return propertiesLoader.getProperty("aws.protocol", "HTTP");
    }

    public int getRetryDelayInMillis() {
        return propertiesLoader.getProperty("aws.retry.delay.millis", 100);
    }

    public int getRetryMaxDelayInMillis() {
        return propertiesLoader.getProperty("aws.retry.max.delay.millis", 20000);
    }

    public int getRetryUnknownHostDelayInMillis() {
        return propertiesLoader.getProperty("aws.retry.unknown.host.delay.millis", 5000);
    }

    public String getSigningRegion() {
        return propertiesLoader.getProperty("aws.signing_region", "us-east-1");
    }

}