package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.s3.S3Config;
import com.flightstats.hub.dao.s3.S3ContentDao;
import com.flightstats.hub.dao.s3.S3WriterManager;
import com.flightstats.hub.group.DynamoGroupDao;
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
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(SpokeTtlEnforcer.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(DynamoChannelConfigurationDao.class);

        bind(ContentService.class).to(AwsContentService.class).asEagerSingleton();
        bind(FileSpokeStore.class).asEagerSingleton();
        bind(RemoteSpokeStore.class).asEagerSingleton();
        bind(SpokeCluster.class).to(CuratorSpokeCluster.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.CACHE))
                .to(SpokeContentDao.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.LONG_TERM))
                .to(S3ContentDao.class).asEagerSingleton();

        bind(DynamoUtils.class).asEagerSingleton();
        bind(GroupDao.class).to(DynamoGroupDao.class).asEagerSingleton();
        bind(S3WriterManager.class).asEagerSingleton();
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
