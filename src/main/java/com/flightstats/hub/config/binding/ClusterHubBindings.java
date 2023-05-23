package com.flightstats.hub.config.binding;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.AppUrlCheck;
import com.flightstats.hub.cluster.SpokeDecommissionManager;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.ClusterServicesRegistration;
import com.flightstats.hub.config.ServiceRegistration;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.AwsProperties;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.ReadOnlyContentDao;
import com.flightstats.hub.dao.ReadOnlyDao;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.ClusterContentService;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDao;
import com.flightstats.hub.dao.aws.DynamoUtils;
import com.flightstats.hub.dao.aws.DynamoWebhookDao;
import com.flightstats.hub.dao.aws.HubS3Client;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.dao.aws.S3BatchContentDao;
import com.flightstats.hub.dao.aws.S3BatchManager;
import com.flightstats.hub.dao.aws.S3MaintenanceManager;
import com.flightstats.hub.dao.aws.S3LargeContentDao;
import com.flightstats.hub.dao.aws.S3SingleContentDao;
import com.flightstats.hub.dao.aws.S3Verifier;
import com.flightstats.hub.dao.aws.S3WriteQueue;
import com.flightstats.hub.dao.aws.S3WriteQueueLifecycle;
import com.flightstats.hub.dao.aws.writeQueue.NoOpWriteQueue;
import com.flightstats.hub.dao.aws.writeQueue.WriteQueue;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.spoke.SpokeContentDao;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClusterHubBindings extends AbstractModule {

    @Override
    protected void configure() {

        bind(LargeContentUtils.class).asEagerSingleton();
        bind(ContentService.class)
                .to(ClusterContentService.class).asEagerSingleton();
        bind(DynamoUtils.class).asEagerSingleton();
        bind(AppUrlCheck.class).asEagerSingleton();

        bind(SpokeDecommissionManager.class).asEagerSingleton();

        bind(PeriodicMetricEmitter.class).asEagerSingleton();
        bind(PeriodicMetricEmitterLifecycle.class).asEagerSingleton();

        bind(ServiceRegistration.class).to(ClusterServicesRegistration.class);

        bind(S3MaintenanceManager.class).asEagerSingleton();
        bind(S3WriteQueueLifecycle.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
        bind(S3AccessMonitor.class).asEagerSingleton();
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
                awsProperties.getProtocol()
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
                awsProperties.getProtocol()
        );
    }

    @Named("READ")
    @Provides
    public static SpokeTtlEnforcer spokeTtlEnforcerRead(ChannelService channelService,
                                                        SpokeContentDao spokeContentDao,
                                                        StatsdReporter statsdReporter,
                                                        SpokeProperties spokeProperties,
                                                        TtlEnforcer ttlEnforcer) {
        return new SpokeTtlEnforcer(SpokeStore.READ, channelService, spokeContentDao, statsdReporter, spokeProperties, ttlEnforcer);
    }

    @Named("WRITE")
    @Provides
    public static SpokeTtlEnforcer spokeTtlEnforcerWrite(ChannelService channelService,
                                                         SpokeContentDao spokeContentDao,
                                                         StatsdReporter statsdReporter,
                                                         SpokeProperties spokeProperties,
                                                         TtlEnforcer ttlEnforcer) {
        return new SpokeTtlEnforcer(SpokeStore.WRITE, channelService, spokeContentDao, statsdReporter, spokeProperties, ttlEnforcer);
    }

    @Singleton
    @Provides
    public WriteQueue buildWriteQueue(AppProperties props, S3WriteQueue s3Queue) {
        return props.isReadOnly() ? new NoOpWriteQueue() : s3Queue;
    }

    @Singleton
    @Provides
    @Named(ContentDao.SINGLE_LONG_TERM)
    public ContentDao buildSingleLongTermContentDao(S3SingleContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named(ContentDao.LARGE_PAYLOAD)
    public ContentDao buildLargePayloadContentDao(S3LargeContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named(ContentDao.BATCH_LONG_TERM)
    public ContentDao buildBatchLongTermContentDao(S3BatchContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, DynamoChannelConfigDao dao, AppProperties appProperties) {
        Dao<ChannelConfig> cfgDao = new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
        return appProperties.isReadOnly() ? new ReadOnlyDao<>(cfgDao) : cfgDao;
    }

    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, DynamoWebhookDao dao, AppProperties appProperties) {
        Dao<Webhook> whDao = new CachedDao<>(dao, watchManager, "/webhooks/cache");
        return appProperties.isReadOnly() ? new ReadOnlyDao<>(whDao) : whDao;
    }

    @Provides
    @Singleton
    public AmazonDynamoDB buildDynamoClient(AwsConnectorFactory factory) {
        return factory.getDynamoClient();
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
        return new HubS3Client(s3Properties, s3Client, statsdReporter);
    }

    @Provides
    @Singleton
    @Named("DISASTER_RECOVERY")
    public HubS3Client getDisasterRecoveryHubS3Client(S3Properties s3Properties, @Named("DISASTER_RECOVERY") AmazonS3 s3Client, StatsdReporter statsdReporter) {
        return new HubS3Client(s3Properties, s3Client, statsdReporter);
    }

}

