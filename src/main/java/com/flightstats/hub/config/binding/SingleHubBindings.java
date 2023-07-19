package com.flightstats.hub.config.binding;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.ServiceRegistration;
import com.flightstats.hub.config.SingleServicesRegistration;
import com.flightstats.hub.config.properties.AwsProperties;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.HubS3Client;
import com.flightstats.hub.dao.file.FileChannelConfigurationDao;
import com.flightstats.hub.dao.file.FileWebhookDao;
import com.flightstats.hub.dao.file.SingleContentService;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.ChannelTtlEnforcer;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class SingleHubBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(ContentService.class).to(SingleContentService.class).asEagerSingleton();
        bind(ChannelTtlEnforcer.class).asEagerSingleton();
        bind(ServiceRegistration.class).to(SingleServicesRegistration.class);
    }

    @Named("MAIN")
    @Singleton
    @Provides
    public static AwsConnectorFactory awsConnector(
            AwsProperties awsProperties,
            DynamoProperties dynamoProperties,
            S3Properties s3Properties) {
        return new AwsConnectorFactory(
                awsProperties,
                dynamoProperties,
                s3Properties,
                awsProperties.getSigningRegion(),
                "HTTP"
        );
    }

    @Named("DISASTER_RECOVERY")
    @Singleton
    @Provides
    public static AwsConnectorFactory disasterRecoveryAwsConnector(
            AwsProperties awsProperties,
            DynamoProperties dynamoProperties,
            S3Properties s3Properties) {
        return new AwsConnectorFactory(
                awsProperties,
                dynamoProperties,
                s3Properties,
                awsProperties.getDisasterRecoveryRegion(),
                "HTTP"
        );
    }

    @Provides
    @Singleton
    @Named("MAIN")
    public AmazonS3 buildS3Client(@Named("MAIN") AwsConnectorFactory factory) {
        return factory.getS3Client();
    }

    @Provides
    @Singleton
    @Named("DISASTER_RECOVERY")
    public AmazonS3 buildDisasterRecoveryS3Client(@Named("DISASTER_RECOVERY") AwsConnectorFactory factory) {
        return factory.getS3Client();
    }

    @Provides
    @Singleton
    @Named("MAIN")
    public HubS3Client getHubS3Client(S3Properties s3Properties, @Named("MAIN") AmazonS3 s3Client, StatsdReporter statsdReporter) {
        HubS3Client.performUglyLegacySanityCheckOnBucketName(s3Client, s3Properties.getBucketName());
        return new HubS3Client(s3Client, statsdReporter);
    }

    @Provides
    @Singleton
    @Named("DISASTER_RECOVERY")
    public HubS3Client getDisasterRecoveryHubS3Client(@Named("DISASTER_RECOVERY") AmazonS3 s3Client, StatsdReporter statsdReporter) {
        return new HubS3Client(s3Client, statsdReporter);
    }

    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, FileChannelConfigurationDao dao) {
        return new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
    }

    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, FileWebhookDao dao) {
        return new CachedDao<>(dao, watchManager, "/webhooks/cache");
    }
}
