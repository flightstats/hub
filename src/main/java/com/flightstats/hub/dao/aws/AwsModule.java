package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.config.metrics.HubInstrumentedResourceMethodDispatchAdapter;
import com.flightstats.hub.app.config.metrics.HubMethodTimingAdapterProvider;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.encryption.AuditChannelService;
import com.flightstats.hub.dao.encryption.BasicChannelService;
import com.flightstats.hub.dao.s3.S3Config;
import com.flightstats.hub.dao.s3.S3ContentDao;
import com.flightstats.hub.dao.s3.S3WriterManager;
import com.flightstats.hub.group.*;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.replication.*;
import com.flightstats.hub.service.ChannelValidator;
import com.flightstats.hub.service.HubHealthCheck;
import com.flightstats.hub.service.HubHealthCheckImpl;
import com.flightstats.hub.spoke.*;
import com.flightstats.hub.time.TimeMonitor;
import com.flightstats.hub.websocket.WebsocketPublisher;
import com.flightstats.hub.websocket.WebsocketPublisherImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

//todo - gfm - 11/13/14 - rename this to something other than Aws
public class AwsModule extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsModule.class);

    private final Properties properties;

    public AwsModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);
        bind(HubHealthCheck.class).to(HubHealthCheckImpl.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(ReplicationService.class).to(ReplicationServiceImpl.class).asEagerSingleton();
        bind(Replicator.class).to(ReplicatorImpl.class).asEagerSingleton();
        bind(ChannelUtils.class).asEagerSingleton();
        bind(CuratorLock.class).asEagerSingleton();
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(SpokeTtlEnforcer.class).asEagerSingleton();

        if (Boolean.parseBoolean(properties.getProperty("app.encrypted"))) {
            logger.info("using encrypted hub");
            bind(ChannelService.class).annotatedWith(BasicChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
            bind(ChannelService.class).to(AuditChannelService.class).asEagerSingleton();
        } else {
            logger.info("using normal hub");
            bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        }
        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(DynamoChannelConfigurationDao.class);
        bind(WebsocketPublisher.class).to(WebsocketPublisherImpl.class).asEagerSingleton();
        bind(ReplicationDao.class).to(CachedReplicationDao.class).asEagerSingleton();
        bind(ReplicationDao.class)
                .annotatedWith(Names.named(CachedReplicationDao.DELEGATE))
                .to(DynamoReplicationDao.class).asEagerSingleton();

        bind(ContentService.class).to(ContentServiceImpl.class).asEagerSingleton();

        bind(FileSpokeStore.class).asEagerSingleton();
        bind(RemoteSpokeStore.class).asEagerSingleton();
        bind(SpokeCluster.class).to(CuratorSpokeCluster.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.CACHE))
                .to(SpokeContentDao.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.LONG_TERM))
                .to(S3ContentDao.class).asEagerSingleton();
        bind(S3WriterManager.class).asEagerSingleton();

        bind(DynamoUtils.class).asEagerSingleton();
        bind(DynamoGroupDao.class).asEagerSingleton();
        bind(ChannelValidator.class).asEagerSingleton();
        bind(GroupValidator.class).asEagerSingleton();
        bind(GroupCallback.class).to(GroupCallbackImpl.class).asEagerSingleton();
        bind(GroupContentKey.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();

        bind(HostedGraphiteSender.class).asEagerSingleton();
        bind(HubInstrumentedResourceMethodDispatchAdapter.class).toProvider(HubMethodTimingAdapterProvider.class).in(Singleton.class);
        bind(TimeMonitor.class).asEagerSingleton();

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
