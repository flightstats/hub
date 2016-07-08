package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.*;
import com.flightstats.hub.spoke.*;
import com.flightstats.hub.webhook.GroupDao;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AwsBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsBindings.class);

    @Override
    protected void configure() {
        if (isBatch()) {
            logger.info("starting server {} as batch", HubHost.getLocalName());
            HubProperties.setProperty("group.keepLeadershipRate", "0.999");
            HubProperties.setProperty("s3Verifier.keepLeadershipRate", "0.999");
            bind(FinalCheck.class).to(PassFinalCheck.class).asEagerSingleton();
        } else {
            logger.info("starting server {} as normal", HubHost.getLocalName());
            bind(SpokeTtlEnforcer.class).asEagerSingleton();
            bind(FileSpokeStore.class).asEagerSingleton();
            bind(SpokeClusterRegister.class).asEagerSingleton();
            bind(FinalCheck.class).to(SpokeFinalCheck.class).asEagerSingleton();
        }
        bind(ChannelService.class).to(GlobalChannelService.class).asEagerSingleton();
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
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
    }

    private static boolean isBatch() {
        String role = HubProperties.getProperty("role." + HubHost.getLocalName(), "all");
        return "batch".equals(role) || StringUtils.contains(HubHost.getLocalName(), "batch");
    }

    static String packages() {
        if (isBatch()) {
            return "com.flightstats.hub.app," +
                    "com.flightstats.hub.health," +
                    "com.flightstats.hub.metrics," +
                    "com.flightstats.hub.time," +
                    "com.flightstats.hub.filter,";
        } else {
            return "com.flightstats.hub";
        }
    }

    @Named("SpokeCuratorCluster")
    @Singleton
    @Provides
    public static CuratorCluster buildSpokeCuratorCluster(CuratorFramework curator) throws Exception {
        return new CuratorCluster(curator, "/SpokeCluster", false);
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
