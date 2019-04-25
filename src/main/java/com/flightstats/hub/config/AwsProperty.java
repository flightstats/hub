package com.flightstats.hub.config;

import javax.inject.Inject;

public class AwsProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public AwsProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getCredentialsFile() {
        return this.propertyLoader.getProperty("aws.credentials", "hub_test_credentials.properties");
    }

    public String getProtocol() {
        return this.propertyLoader.getProperty("aws.protocol", "HTTP");
    }

    public int getRetryDelayInMillis() {
        return this.propertyLoader.getProperty("aws.retry.delay.millis", 100);
    }

    public int getRetryMaxDelayInMillis() {
        return this.propertyLoader.getProperty("aws.retry.max.delay.millis", 20000);
    }

    public int getRetryUnknownHostDelayInMillis() {
        return this.propertyLoader.getProperty("aws.retry.unknown.host.delay.millis", 5000);
    }

    public String getSigningRegion() {
        return this.propertyLoader.getProperty("aws.signing_region", "us-east-1");
    }

}