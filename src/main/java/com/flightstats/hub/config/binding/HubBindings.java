package com.flightstats.hub.config.binding;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flightstats.hub.app.FinalCheck;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.app.PermissionsChecker;
import com.flightstats.hub.app.ShutdownManager;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.cluster.DecommissionCluster;
import com.flightstats.hub.cluster.HubClusterRegister;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.config.SystemProperties;
import com.flightstats.hub.config.ZookeeperProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierConfigProvider;
import com.flightstats.hub.events.EventsService;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterProvider;
import com.flightstats.hub.metrics.MetricRegistryProvider;
import com.flightstats.hub.metrics.MetricsConfig;
import com.flightstats.hub.metrics.MetricsConfigProvider;
import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterProvider;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.replication.ReplicationManager;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.rest.RetryClientFilter;
import com.flightstats.hub.rest.Rfc3339DateSerializer;
import com.flightstats.hub.spoke.ClusterWriteSpoke;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.GCRunner;
import com.flightstats.hub.spoke.LocalReadSpoke;
import com.flightstats.hub.spoke.ReadOnlyClusterSpokeStore;
import com.flightstats.hub.spoke.SpokeChronologyStore;
import com.flightstats.hub.spoke.SpokeClusterHealthCheck;
import com.flightstats.hub.spoke.SpokeClusterRegister;
import com.flightstats.hub.spoke.SpokeFinalCheck;
import com.flightstats.hub.spoke.SpokeManager;
import com.flightstats.hub.spoke.SpokeReadContentDao;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeStoreConfig;
import com.flightstats.hub.spoke.SpokeWriteContentDao;
import com.flightstats.hub.spoke.SpokeWriteStoreConfigProvider;
import com.flightstats.hub.time.NtpMonitor;
import com.flightstats.hub.time.TimeService;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.SecretFilter;
import com.flightstats.hub.util.StaleEntity;
import com.flightstats.hub.webhook.WebhookManager;
import com.flightstats.hub.webhook.WebhookValidator;
import com.flightstats.hub.ws.WebSocketService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.sun.jersey.api.client.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;

import javax.websocket.WebSocketContainer;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.Constants.READ;
import static com.flightstats.hub.util.Constants.READ_CACHE;
import static com.flightstats.hub.util.Constants.S3_VERIFIER_CHANNEL_THREAD_POOL;
import static com.flightstats.hub.util.Constants.S3_VERIFIER_QUERY_THREAD_POOL;
import static com.flightstats.hub.util.Constants.WRITE;
import static com.flightstats.hub.util.Constants.WRITE_CACHE;

@Slf4j
public class HubBindings extends AbstractModule {

    @Singleton
    @Provides
    public static CuratorFramework buildCurator(ZooKeeperState zooKeeperState,
                                                AppProperties appProperties,
                                                ZookeeperProperties zookeeperProperties) {

        log.info("connecting to zookeeper(s) at {} with name {} env {}",
                zookeeperProperties.getConnection(),
                appProperties.getAppName(),
                appProperties.getEnv());

        FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider(zookeeperProperties.getConnection());
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .namespace(appProperties.getAppName() + "-" + appProperties.getEnv())
                .ensembleProvider(ensembleProvider)
                .retryPolicy(buildRetryPolicy(zookeeperProperties)).build();
        curatorFramework.getConnectionStateListenable().addListener(zooKeeperState.getStateListener());
        curatorFramework.start();

        try {
            curatorFramework.checkExists().forPath("/startup");
        } catch (Exception e) {
            log.warn("unable to access zookeeper");
            throw new RuntimeException("unable to access zookeeper");
        }
        return curatorFramework;
    }

    private static RetryPolicy buildRetryPolicy(ZookeeperProperties zookeeperProperties) {
        return new BoundedExponentialBackoffRetry(
                zookeeperProperties.getBaseSleepTimeInMillis(),
                zookeeperProperties.getMaxSleepTimeInMillis(),
                zookeeperProperties.getMaxRetries());
    }

    @Singleton
    @Provides
    public static Client buildJerseyClient(SystemProperties systemProperties) {
        return create(systemProperties, true);
    }

    @Named("NoRedirects")
    @Singleton
    @Provides
    public static Client buildJerseyClientNoRedirects(SystemProperties systemProperties) {
        return create(systemProperties, false);
    }

    private static Client create(SystemProperties systemProperties, boolean followRedirects) {
        int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(systemProperties.getHttpConnectTimeoutInSec());
        int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(systemProperties.getHttpReadTimeoutInSec());
        Client client = RestClient.createClient(connectTimeoutMillis, readTimeoutMillis, followRedirects, true);
        client.addFilter(new RetryClientFilter(systemProperties));
        return client;
    }

    @Named("HubCluster")
    @Singleton
    @Provides
    public static Cluster buildHubCluster(CuratorFramework curator,
                                          AppProperties appProperties,
                                          SpokeProperties spokeProperties) throws Exception {
        return new CuratorCluster(curator,
                "/HubCluster",
                true,
                false,
                new DecommissionCluster() {
                },
                appProperties,
                spokeProperties);
    }

    @Named("SpokeCuratorCluster")
    @Singleton
    @Provides
    public static CuratorCluster buildSpokeCuratorCluster(@Named("SpokeCluster") Cluster cluster) {
        return (CuratorCluster) cluster;
    }

    @Named("HubCuratorCluster")
    @Singleton
    @Provides
    public static CuratorCluster buildHubCuratorCluster(@Named("HubCluster") Cluster cluster) {
        return (CuratorCluster) cluster;
    }

    @Named("SpokeCluster")
    @Singleton
    @Provides
    public static Cluster buildSpokeCluster(CuratorFramework curator,
                                            SpokeDecommissionCluster spokeDecommissionCluster,
                                            AppProperties appProperties,
                                            SpokeProperties spokeProperties) throws Exception {
        return new CuratorCluster(
                curator,
                "/SpokeCluster",
                false,
                true,
                spokeDecommissionCluster,
                appProperties,
                spokeProperties);
    }

    @Singleton
    @Provides
    public static WebSocketContainer buildWebSocketContainer() throws Exception {
        ClientContainer container = new ClientContainer();
        container.start();
        return container;
    }

    @Singleton
    @Provides
    public static ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    @Named(S3_VERIFIER_CHANNEL_THREAD_POOL)
    @Singleton
    @Provides
    public static ExecutorService channelThreadPool(VerifierConfig verifierConfig) {
        return Executors.newFixedThreadPool(verifierConfig.getChannelThreads(), new ThreadFactoryBuilder().setNameFormat("S3VerifierChannel-%d").build());
    }

    @Named(S3_VERIFIER_QUERY_THREAD_POOL)
    @Singleton
    @Provides
    public
    static ExecutorService queryThreadPool(VerifierConfig verifierConfig) {
        return Executors.newFixedThreadPool(verifierConfig.getQueryThreads(), new ThreadFactoryBuilder().setNameFormat("S3VerifierQuery-%d").build());
    }

    @Singleton
    @Provides
    public ClusterWriteSpoke buildClusterWriterSpokeStore(SpokeManager store, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyClusterSpokeStore(store) : store;
    }

    @Named(WRITE_CACHE)
    @Provides
    @Singleton
    public ContentDao contentDao(ClusterWriteSpoke writeSpoke, SpokeChronologyStore chronoStore, SpokeProperties spokeProperties) {
        return new SpokeWriteContentDao(writeSpoke, chronoStore, spokeProperties);
    }

    @Named(READ_CACHE)
    @Provides
    @Singleton
    public ContentDao contentDao(LocalReadSpoke localReadSpoke) {
        return new SpokeReadContentDao(localReadSpoke);
    }

    @Named(WRITE)
    @Provides
    public FileSpokeStore fileSpokeStoreWrite(SpokeProperties spokeProperties) {
        return new FileSpokeStore(
                spokeProperties.getPath(SpokeStore.WRITE),
                spokeProperties.getTtlMinutes(SpokeStore.WRITE));
    }

    @Named(READ)
    @Provides
    public FileSpokeStore fileSpokeStoreRead(SpokeProperties spokeProperties) {
        return new FileSpokeStore(
                spokeProperties.getPath(SpokeStore.READ),
                spokeProperties.getTtlMinutes(SpokeStore.READ));
    }

    @Override

    protected void configure() {

        bind(SecretFilter.class).asEagerSingleton();
        bind(HubHealthCheck.class).asEagerSingleton();
        bind(HubClusterRegister.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(HubUtils.class).asEagerSingleton();
        bind(GCRunner.class).asEagerSingleton();
        bind(LastContentPath.class).asEagerSingleton();
        bind(NtpMonitor.class).asEagerSingleton();
        bind(StaleEntity.class).asEagerSingleton();

        bind(FinalCheck.class).to(SpokeFinalCheck.class).asEagerSingleton();
        bind(HubVersion.class).asEagerSingleton();

        bind(ReplicationManager.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();
        bind(WebhookManager.class).asEagerSingleton();
        bind(SpokeManager.class).asEagerSingleton();
        bind(ShutdownManager.class).asEagerSingleton();

        bind(ChannelValidator.class).asEagerSingleton();
        bind(WebhookValidator.class).asEagerSingleton();

        bind(ContentRetriever.class).asEagerSingleton();

        bind(ChannelService.class).asEagerSingleton();
        bind(InFlightService.class).asEagerSingleton();
        bind(TagService.class).asEagerSingleton();
        bind(TimeService.class).asEagerSingleton();
        bind(EventsService.class).asEagerSingleton();
        bind(WebSocketService.class);

        bind(HubVersion.class).asEagerSingleton();
        bind(LocalReadSpoke.class).to(SpokeManager.class);
        bind(SpokeChronologyStore.class).to(SpokeManager.class);
        bind(SpokeClusterHealthCheck.class).to(SpokeManager.class);
        bind(SpokeClusterRegister.class).asEagerSingleton();
        bind(PermissionsChecker.class).asEagerSingleton();

        // metrics
        bind(MetricsConfig.class).toProvider(MetricsConfigProvider.class).asEagerSingleton();
        bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class).asEagerSingleton();
        bind(ScheduledReporter.class).toProvider(InfluxdbReporterProvider.class).asEagerSingleton();
        bind(InfluxdbReporterLifecycle.class).asEagerSingleton();

        bind(StatsDFilter.class).asEagerSingleton();
        bind(StatsdReporter.class).toProvider(StatsDReporterProvider.class).asEagerSingleton();
        bind(StatsDReporterLifecycle.class).asEagerSingleton();
        bind(CustomMetricsLifecycle.class).asEagerSingleton();

        bind(VerifierConfig.class)
                .toProvider(VerifierConfigProvider.class)
                .asEagerSingleton();

        bind(SpokeStoreConfig.class)
                .annotatedWith(Names.named("spokeWriteStoreConfig"))
                .toProvider(SpokeWriteStoreConfigProvider.class)
                .asEagerSingleton();
    }
}
