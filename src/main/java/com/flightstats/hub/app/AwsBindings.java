package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.*;
import com.flightstats.hub.group.GroupDao;
import com.flightstats.hub.spoke.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AwsBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsBindings.class);

    @Override
    protected void configure() {
        String role = getRole();
        logger.info("starting server {}  with role {}", HubHost.getLocalName(), role);
        if ("batch".equals(role)) {
            HubProperties.setProperty("group.keepLeadershipRate", "0.999");
        } else {
            bind(SpokeTtlEnforcer.class).asEagerSingleton();
            bind(FileSpokeStore.class).asEagerSingleton();
            bind(CuratorSpokeCluster.class).asEagerSingleton();
            bind(S3WriterManager.class).asEagerSingleton();
            bind(SpokeHealth.class).asEagerSingleton();
        }
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(ChannelConfigDao.class).to(CachedChannelConfigDao.class).asEagerSingleton();
        bind(ChannelConfigDao.class)
                .annotatedWith(Names.named(CachedChannelConfigDao.DELEGATE))
                .to(DynamoChannelConfigDao.class);
        bind(ContentService.class).to(AwsContentService.class).asEagerSingleton();
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
        bind(GroupDao.class).to(DynamoGroupDao.class).asEagerSingleton();
    }

    private static String getRole() {
        return HubProperties.getProperty("role." + HubHost.getLocalName(), "all");
    }

    public static String packages() {
        String role = getRole();
        if ("batch".equals(role)) {
            return "com.flightstats.hub.app," +
                    "com.flightstats.hub.health,";
        } else {
            return "com.flightstats.hub";
        }
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
