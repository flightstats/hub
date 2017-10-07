package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.SpokeDecommissionManager;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.*;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.RemoteSpokeStore;
import com.flightstats.hub.spoke.SpokeBatchContentDao;
import com.flightstats.hub.spoke.SpokeSingleContentDao;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ClusterHubBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(ClusterHubBindings.class);

    @Override
    protected void configure() {
        logger.info("starting server {} ", HubHost.getLocalName());

        bind(ChannelService.class).to(ChannelNameService.class).asEagerSingleton();
        bind(ChannelService.class)
                .annotatedWith(Names.named(ChannelService.DELEGATE))
                .to(GlobalChannelService.class).asEagerSingleton();
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(ContentService.class)
                .to(ClusterContentService.class).asEagerSingleton();
        bind(RemoteSpokeStore.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.SINGLE_CACHE))
                .to(SpokeSingleContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.BATCH_CACHE))
                .to(SpokeBatchContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.SINGLE_LONG_TERM))
                .to(S3SingleContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.BATCH_LONG_TERM))
                .to(S3BatchContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.LARGE_PAYLOAD))
                .to(S3LargeContentDao.class).asEagerSingleton();
        bind(DynamoUtils.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
        bind(AppUrlCheck.class).asEagerSingleton();

        bind(SpokeTtlEnforcer.class)
                .annotatedWith(Names.named(FileSpokeStore.SINGLE))
                .toInstance(new SpokeTtlEnforcer(
                        HubProperties.getSpokePath("single"),
                        HubProperties.getSpokeTtlMinutes("single")));

        bind(SpokeTtlEnforcer.class)
                .annotatedWith(Names.named(FileSpokeStore.BATCH))
                .toInstance(new SpokeTtlEnforcer(
                        HubProperties.getSpokePath("batch"),
                        HubProperties.getSpokeTtlMinutes("batch")));

        bind(DocumentationDao.class).to(S3DocumentationDao.class).asEagerSingleton();
        bind(SpokeDecommissionManager.class).asEagerSingleton();
    }

    @Inject
    @Singleton
    @Provides
    @Named(FileSpokeStore.SINGLE)
    public static FileSpokeStore buildSingleSpokeStore() {
        String spokePath = HubProperties.getSpokePath("single");
        int spokeTtlMinutes = HubProperties.getSpokeTtlMinutes("single");
        return new FileSpokeStore(spokePath, spokeTtlMinutes);
    }

    @Inject
    @Singleton
    @Provides
    @Named(FileSpokeStore.BATCH)
    public static FileSpokeStore buildBatchSpokeStore() {
        String spokePath = HubProperties.getSpokePath("batch");
        int spokeTtlMinutes = HubProperties.getSpokeTtlMinutes("batch");
        return new FileSpokeStore(spokePath, spokeTtlMinutes);
    }

    @Inject
    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, DynamoChannelConfigDao dao) {
        return new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
    }

    @Inject
    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, DynamoWebhookDao dao) {
        return new CachedDao<>(dao, watchManager, "/webhooks/cache");
    }

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDB buildDynamoClient(AwsConnectorFactory factory) throws IOException {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }

}
