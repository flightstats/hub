package com.flightstats.hub.config;

import javax.inject.Inject;

public class DynamoProperties {

    private final PropertiesLoader propertiesLoader;
    private final AppProperties appProperties;

    @Inject
    public DynamoProperties(PropertiesLoader propertiesLoader, AppProperties appProperties) {
        this.propertiesLoader = propertiesLoader;
        this.appProperties = appProperties;
    }

    private String getLegacyChannelTableName() {
        return appProperties.getAppName() + "-" + appProperties.getEnv() + "-" + "channelMetaData";
    }

    private String getLegacyWebhookTableName() {
        return appProperties.getAppName() + "-" + appProperties.getEnv() + "-" + "GroupConfig";
    }

    public String getWebhookConfigTableName(){
        return this.propertiesLoader.getProperty("dynamo.table_name.webhook_configs", getLegacyWebhookTableName());
    }

    public String getChannelConfigTableName(){
        return this.propertiesLoader.getProperty("dynamo.table_name.channel_configs", getLegacyChannelTableName());
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

}