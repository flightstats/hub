package com.flightstats.hub.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterProvider;
import com.flightstats.hub.metrics.MetricRegistryProvider;
import com.flightstats.hub.metrics.MetricsConfigProvider;
import com.flightstats.hub.metrics.MetricsConfig;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterProvider;
import com.flightstats.hub.replication.ReplicationManager;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.rest.RetryClientFilter;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.Rfc3339DateSerializer;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.GCRunner;
import com.flightstats.hub.spoke.SpokeClusterRegister;
import com.flightstats.hub.spoke.SpokeFinalCheck;
import com.flightstats.hub.spoke.SpokeReadContentDao;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeWriteContentDao;
import com.flightstats.hub.time.NtpMonitor;
import com.flightstats.hub.time.TimeService;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.WebhookManager;
import com.flightstats.hub.webhook.WebhookValidator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
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
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HubBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(HubBindings.class);

    @Singleton
    @Provides
    public static CuratorFramework buildCurator(@Named("app.name") String appName, @Named("app.environment") String environment,
                                                @Named("zookeeper.connection") String zkConnection,
                                                ZooKeeperState zooKeeperState) {
        logger.info("connecting to zookeeper(s) at {} with name {} env {}", zkConnection, appName, environment);
        FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider(zkConnection);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .namespace(appName + "-" + environment)
                .ensembleProvider(ensembleProvider)
                .retryPolicy(buildRetryPolicy()).build();
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

    private static RetryPolicy buildRetryPolicy() {
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
        Client client = RestClient.createClient(connectTimeoutMillis, readTimeoutMillis, followRedirects, true);
        client.addFilter(new RetryClientFilter());
        return client;
    }

    @Named("HubCluster")
    @Singleton
    @Provides
    public static Cluster buildHubCluster(CuratorFramework curator) throws Exception {
        return new CuratorCluster(curator, "/HubCluster", true, false, new DecommissionCluster() {
        });
    }

    @Named("HubCuratorCluster")
    @Singleton
    @Provides
    public static CuratorCluster buildHubCuratorCluster(@Named("HubCluster") Cluster cluster) throws Exception {
        return (CuratorCluster) cluster;
    }

    @Named("SpokeCluster")
    @Singleton
    @Provides
    public static Cluster buildSpokeCluster(CuratorFramework curator, SpokeDecommissionCluster spokeDecommissionCluster) throws Exception {
        return new CuratorCluster(curator, "/SpokeCluster", false, true, spokeDecommissionCluster);
    }

    @Named("SpokeCuratorCluster")
    @Singleton
    @Provides
    public static CuratorCluster buildSpokeCuratorCluster(@Named("SpokeCluster") Cluster cluster) throws Exception {
        return (CuratorCluster) cluster;
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

    @Override
    protected void configure() {
        Names.bindProperties(binder(), HubProperties.getProperties());

        bind(HubHealthCheck.class).asEagerSingleton();
        bind(HubClusterRegister.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(ReplicationManager.class).asEagerSingleton();
        bind(HubUtils.class).asEagerSingleton();
        bind(GCRunner.class).asEagerSingleton();
        bind(ChannelValidator.class).asEagerSingleton();
        bind(WebhookValidator.class).asEagerSingleton();
        bind(WebhookManager.class).asEagerSingleton();
        bind(LastContentPath.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();
        bind(NtpMonitor.class).asEagerSingleton();
        bind(TimeService.class).asEagerSingleton();
        bind(ShutdownManager.class).asEagerSingleton();
        bind(SpokeClusterRegister.class).asEagerSingleton();
        bind(FinalCheck.class).to(SpokeFinalCheck.class).asEagerSingleton();
        bind(InFlightService.class).asEagerSingleton();
        bind(ChannelService.class).asEagerSingleton();
        bind(HubVersion.class).toInstance(new HubVersion());

        // metrics
        bind(MetricsConfig.class).toProvider(MetricsConfigProvider.class).asEagerSingleton();
        bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class).asEagerSingleton();
        bind(ScheduledReporter.class).toProvider(InfluxdbReporterProvider.class).asEagerSingleton();
        bind(InfluxdbReporterLifecycle.class).asEagerSingleton();

        bind(StatsDFilter.class).asEagerSingleton();
        bind(StatsdReporter.class).toProvider(StatsDReporterProvider.class).asEagerSingleton();
        bind(StatsDReporterLifecycle.class).asEagerSingleton();
        bind(CustomMetricsLifecycle.class).asEagerSingleton();
        bind(PeriodicMetricEmitter.class).asEagerSingleton();
        bind(PeriodicMetricEmitterLifecycle.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.WRITE_CACHE))
                .to(SpokeWriteContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.READ_CACHE))
                .to(SpokeReadContentDao.class).asEagerSingleton();

        bind(FileSpokeStore.class)
                .annotatedWith(Names.named(SpokeStore.WRITE.name()))
                .toInstance(new FileSpokeStore(
                        HubProperties.getSpokePath(SpokeStore.WRITE),
                        HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE)));

        bind(FileSpokeStore.class)
                .annotatedWith(Names.named(SpokeStore.READ.name()))
                .toInstance(new FileSpokeStore(
                        HubProperties.getSpokePath(SpokeStore.READ),
                        HubProperties.getSpokeTtlMinutes(SpokeStore.READ)));
    }

}
