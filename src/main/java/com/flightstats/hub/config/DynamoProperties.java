package com.flightstats.hub.config;

import javax.inject.Inject;

public class DynamoProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public DynamoProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getEndpoint() {
        return this.propertiesLoader.getProperty("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
    }

    public int getTableCreationWaitInMinutes() {
        return this.propertiesLoader.getProperty("dynamo.table_creation_wait_minutes", 10);
    }

    public long getThroughputRead(String type) {
        return this.propertiesLoader.getProperty("dynamo.throughput." + type + ".read", 100);
    }

    public long getThroughputWrite(String type) {
        return this.propertiesLoader.getProperty("dynamo.throughput." + type + ".write", 10);
    }

    public int getMaxConnections() {
        return this.propertiesLoader.getProperty("dynamo.maxConnections", 50);
    }

    public int getConnectionTimeout() {
        return this.propertiesLoader.getProperty("dynamo.connectionTimeout", 10 * 1000);
    }

    public int getSocketTimeout() {
        return this.propertiesLoader.getProperty("dynamo.socketTimeout", 30 * 1000);
    }

    public String getWebhookConfigTableName(String legacyTableName){
        return this.propertiesLoader.getProperty("dynamo.table_name.webhook_configs", legacyTableName);
    }

    public String getChannelConfigTableName(String legacyTableName){
        return this.propertiesLoader.getProperty("dynamo.table_name.channel_configs", legacyTableName);
    }

}