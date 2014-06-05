package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.encryption.AuditChannelService;
import com.flightstats.hub.dao.encryption.BasicChannelService;
import com.flightstats.hub.dao.s3.ContentDaoImpl;
import com.flightstats.hub.dao.s3.S3Config;
import com.flightstats.hub.dao.s3.S3IndexDao;
import com.flightstats.hub.dao.timeIndex.TimeIndexCoordinator;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.group.DynamoGroupDao;
import com.flightstats.hub.group.GroupCallback;
import com.flightstats.hub.group.GroupCallbackImpl;
import com.flightstats.hub.group.GroupValidator;
import com.flightstats.hub.replication.*;
import com.flightstats.hub.service.CreateChannelValidator;
import com.flightstats.hub.util.ContentKeyGenerator;
import com.flightstats.hub.util.CuratorKeyGenerator;
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

public class AwsModule extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsModule.class);

	private final Properties properties;

	public AwsModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(ReplicationService.class).to(ReplicationServiceImpl.class).asEagerSingleton();
        bind(Replicator.class).to(ReplicatorImpl.class).asEagerSingleton();
        bind(TimeIndexCoordinator.class).asEagerSingleton();
        bind(ChannelUtils.class).asEagerSingleton();
        bind(CuratorLock.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        //todo - gfm - 5/30/14 - should all singletons be eager?
		bind(AwsConnectorFactory.class).in(Singleton.class);

        if (Boolean.parseBoolean(properties.getProperty("app.encrypted"))) {
            logger.info("using encrypted hub");
            bind(ChannelService.class).annotatedWith(BasicChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
            bind(ChannelService.class).to(AuditChannelService.class).asEagerSingleton();
        } else {
            logger.info("using normal hub");
            bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        }
        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).in(Singleton.class);
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(DynamoChannelConfigurationDao.class);
        bind(WebsocketPublisher.class).to(WebsocketPublisherImpl.class).asEagerSingleton();
        bind(ReplicationDao.class).to(CachedReplicationDao.class).in(Singleton.class);
        bind(ReplicationDao.class)
                .annotatedWith(Names.named(CachedReplicationDao.DELEGATE))
                .to(DynamoReplicationDao.class);

        bind(ContentService.class).to(ContentServiceImpl.class).in(Singleton.class);
        bind(ContentDao.class).to(ContentDaoImpl.class).in(Singleton.class);
        bind(TimeIndexDao.class).to(S3IndexDao.class).in(Singleton.class);
        bind(LastUpdatedDao.class).to(SequenceLastUpdatedDao.class).in(Singleton.class);
        bind(ContentKeyGenerator.class).to(CuratorKeyGenerator.class).in(Singleton.class);

        bind(DynamoUtils.class).asEagerSingleton();
        bind(DynamoGroupDao.class).asEagerSingleton();
        bind(CreateChannelValidator.class).asEagerSingleton();
        bind(GroupValidator.class).asEagerSingleton();
        bind(GroupCallback.class).to(GroupCallbackImpl.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();
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
