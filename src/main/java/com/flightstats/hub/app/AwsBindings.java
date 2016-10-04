package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.*;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.spoke.RemoteSpokeStore;
import com.flightstats.hub.spoke.SpokeContentDao;
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

class AwsBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsBindings.class);

    @Override
    protected void configure() {
        logger.info("starting server {} ", HubHost.getLocalName());

        bind(ChannelService.class).to(GlobalChannelService.class).asEagerSingleton();
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(ContentService.class).to(CommonContentService.class).asEagerSingleton();
        bind(ContentService.class)
                .annotatedWith(Names.named(ContentService.IMPL))
                .to(SpokeS3ContentService.class).asEagerSingleton();
        bind(RemoteSpokeStore.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.CACHE))
                .to(SpokeContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.SINGLE_LONG_TERM))
                .to(S3SingleContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.BATCH_LONG_TERM))
                .to(S3BatchContentDao.class).asEagerSingleton();
        bind(DynamoUtils.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
    }

    @Inject
    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, DynamoChannelConfigDao dao) {
        return new CachedDao<>(dao, watchManager, "/channels/cache");
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
    public AmazonDynamoDBClient buildDynamoClient(AwsConnectorFactory factory) throws IOException {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }

}
