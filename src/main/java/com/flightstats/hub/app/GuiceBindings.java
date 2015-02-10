package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.LastContentKey;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.encryption.AuditChannelService;
import com.flightstats.hub.dao.encryption.BasicChannelService;
import com.flightstats.hub.dao.s3.S3Config;
import com.flightstats.hub.dao.s3.S3ContentDao;
import com.flightstats.hub.dao.s3.S3WriterManager;
import com.flightstats.hub.group.DynamoGroupDao;
import com.flightstats.hub.group.GroupCallback;
import com.flightstats.hub.group.GroupCallbackImpl;
import com.flightstats.hub.group.GroupValidator;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.metrics.HubInstrumentedResourceMethodDispatchAdapter;
import com.flightstats.hub.metrics.HubMethodTimingAdapterProvider;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.replication.ReplicatorImpl;
import com.flightstats.hub.rest.RetryClientFilter;
import com.flightstats.hub.spoke.*;
import com.flightstats.hub.time.NTPMonitor;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.client.Client;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.WebSocketContainer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class GuiceBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(GuiceBindings.class);

    @Override
    protected void configure() {
        Names.bindProperties(binder(), HubProperties.getProperties());
        bind(HubHealthCheck.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(Replicator.class).to(ReplicatorImpl.class).asEagerSingleton();
        bind(HubUtils.class).asEagerSingleton();
        bind(CuratorLock.class).asEagerSingleton();
        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(SpokeTtlEnforcer.class).asEagerSingleton();

        if (Boolean.parseBoolean(HubProperties.getProperty("app.encrypted", "false"))) {
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
        bind(LastContentKey.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();

        bind(HostedGraphiteSender.class).asEagerSingleton();
        bind(HubInstrumentedResourceMethodDispatchAdapter.class).toProvider(HubMethodTimingAdapterProvider.class).in(Singleton.class);
        bind(NTPMonitor.class).asEagerSingleton();
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

    @Singleton
    @Provides
    public static HazelcastInstance buildHazelcast() throws FileNotFoundException {
        String hazelCastXml = HubProperties.getProperty("hazelcast.conf.xml", "");
        Config config;
        if (Strings.isNullOrEmpty(hazelCastXml)) {
            config = new ClasspathXmlConfig("hazelcast.conf.xml");
        } else {
            config = new FileSystemXmlConfig(hazelCastXml);
        }
        return Hazelcast.newHazelcastInstance(config);
    }

    @Named("ChannelConfigurationMap")
    @Singleton
    @Provides
    public static ConcurrentMap<String, ChannelConfiguration> buildChannelConfigurationMap(HazelcastInstance hazelcast) throws FileNotFoundException {
        return hazelcast.getMap("ChannelConfigurationMap");
    }

    @Singleton
    @Provides
    public static CuratorFramework buildCurator(@Named("app.name") String appName, @Named("app.environment") String environment,
                                                @Named("zookeeper.connection") String zkConnection,
                                                RetryPolicy retryPolicy, ZooKeeperState zooKeeperState) {
        logger.info("connecting to zookeeper(s) at " + zkConnection);
        FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider(zkConnection);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().namespace(appName + "-" + environment)
                .ensembleProvider(ensembleProvider)
                .retryPolicy(retryPolicy).build();
        curatorFramework.getConnectionStateListenable().addListener(zooKeeperState.getStateListener());
        curatorFramework.start();

        try {
            Stat stat = curatorFramework.checkExists().forPath("/startup");
        } catch (Exception e) {
            logger.warn("unable to access zookeeper");
            throw new RuntimeException("unable to access zookeeper");
        }
        return curatorFramework;
    }

    @Singleton
    @Provides
    public static RetryPolicy buildRetryPolicy() {
        return new BoundedExponentialBackoffRetry(
                HubProperties.getProperty("zookeeper.baseSleepTimeMs", 10),
                HubProperties.getProperty("zookeeper.maxSleepTimeMs", 10000),
                HubProperties.getProperty("zookeeper.maxRetries", 20));
    }

    @Singleton
    @Provides
    public static Client buildJerseyClient() {
        return create(true);
    }

    @Named("NoRedirects")
    @Singleton
    @Provides
    public static Client buildJerseyClientNoRedirects() {
        return create(false);
    }

    private static Client create(boolean followRedirects) {
        int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(HubProperties.getProperty("http.connect.timeout.seconds", 30));
        int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(HubProperties.getProperty("http.read.timeout.seconds", 120));

        Client client = Client.create();
        client.setConnectTimeout(connectTimeoutMillis);
        client.setReadTimeout(readTimeoutMillis);
        client.addFilter(new RetryClientFilter());
        client.addFilter(new com.sun.jersey.api.client.filter.GZIPContentEncodingFilter());
        client.setFollowRedirects(followRedirects);
        return client;
    }

    @Singleton
    @Provides
    public static WebSocketContainer buildWebSocketContainer() throws Exception {
        ClientContainer container = new ClientContainer();
        container.start();
        return container;
    }

}
