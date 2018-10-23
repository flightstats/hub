package com.flightstats.hub.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.channel.TimeLinkUtil;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.cluster.DecommissionCluster;
import com.flightstats.hub.cluster.HubClusterRegister;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.DelegatingMetricsService;
import com.flightstats.hub.metrics.MetricsRunner;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.model.HubDateTimeTypeAdapter;
import com.flightstats.hub.model.HubDateTypeAdapter;
import com.flightstats.hub.replication.ReplicationManager;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.rest.RetryClientFilter;
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
import com.flightstats.hub.util.StaleUtil;
import com.flightstats.hub.webhook.WebhookManager;
import com.flightstats.hub.webhook.WebhookValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.apache.zookeeper.ZooKeeperMain;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.joda.time.DateTime;

import javax.websocket.WebSocketContainer;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HubModule extends AbstractModule {

    private final Properties properties;

    public HubModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        log.info("configuring HubModule");

        bind(ZooKeeperInProcess.class).asEagerSingleton();

        // TODO: untangle this crutch
        requestStaticInjection(
                HubHost.class,
                ActiveTraces.class,
                LinkBuilder.class,
                TimeLinkUtil.class,
                StaleUtil.class
        );

        bind(HubHealthCheck.class).asEagerSingleton();
        bind(HubClusterRegister.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(ReplicationManager.class).asEagerSingleton();
        bind(HubUtils.class).asEagerSingleton();
        bind(GCRunner.class).asEagerSingleton();
        bind(MetricsRunner.class).asEagerSingleton();
        bind(ChannelValidator.class).asEagerSingleton();
        bind(WebhookValidator.class).asEagerSingleton();
        bind(WebhookManager.class).asEagerSingleton();
        bind(LastContentPath.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();
        bind(MetricsService.class).to(DelegatingMetricsService.class).asEagerSingleton();
        bind(PeriodicMetricEmitter.class).asEagerSingleton();
        bind(NtpMonitor.class).asEagerSingleton();
        bind(TimeService.class).asEagerSingleton();
        bind(ShutdownManager.class).asEagerSingleton();
        bind(SpokeClusterRegister.class).asEagerSingleton();
        bind(FinalCheck.class).to(SpokeFinalCheck.class).asEagerSingleton();
        bind(InFlightService.class).asEagerSingleton();
        bind(ChannelService.class).asEagerSingleton();

        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.WRITE_CACHE))
                .to(SpokeWriteContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.READ_CACHE))
                .to(SpokeReadContentDao.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public HubProperties provideHubProperties() {
        log.info("providing HubProperties");
        return new HubProperties(properties);
    }

    @Provides
    @Singleton
    @Named("StartTime")
    public DateTime provideStartTime() {
        log.info("providing 'StartTime' DateTime");
        return new DateTime();
    }

    @Provides
    @Singleton
    public CuratorFramework buildCurator(HubProperties hubProperties, ZooKeeperState zooKeeperState) {
        log.info("providing CuratorFramework");
        String appName = hubProperties.getProperty("app.name");
        String environment = hubProperties.getProperty("app.environment");
        String zkConnection = hubProperties.getProperty("zookeeper.connection");
        log.info("connecting to zookeeper(s) at {} with name {} env {}", zkConnection, appName, environment);
        FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider(zkConnection);
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(
                hubProperties.getProperty("zookeeper.baseSleepTimeMs", 10),
                hubProperties.getProperty("zookeeper.maxSleepTimeMs", 10000),
                hubProperties.getProperty("zookeeper.maxRetries", 20));
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .namespace(appName + "-" + environment)
                .ensembleProvider(ensembleProvider)
                .retryPolicy(retryPolicy)
                .build();
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

    @Provides
    @Singleton
    public RetryClientFilter provideRetryClientFilter(HubProperties hubProperties) {
        log.info("providing RetryClientFilter");
        int maxRetries = hubProperties.getProperty("http.maxRetries", 8);
        int sleep = hubProperties.getProperty("http.sleep", 1000);
        return new RetryClientFilter(maxRetries, sleep);
    }

    @Provides
    @Singleton
    public static Client buildJerseyClient(HubProperties hubProperties, RetryClientFilter retryClientFilter) {
        log.info("providing Client");
        int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(hubProperties.getProperty("http.connect.timeout.seconds", 30));
        int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(hubProperties.getProperty("http.read.timeout.seconds", 120));
        Client client = RestClient.createClient(connectTimeoutMillis, readTimeoutMillis, true, true);
        client.addFilter(retryClientFilter);
        return client;
    }

    @Provides
    @Singleton
    @Named("NoRedirects")
    public static Client buildJerseyClientNoRedirects(HubProperties hubProperties, RetryClientFilter retryClientFilter) {
        log.info("providing 'NoRedirects' Client");
        int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(hubProperties.getProperty("http.connect.timeout.seconds", 30));
        int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(hubProperties.getProperty("http.read.timeout.seconds", 120));
        Client client = RestClient.createClient(connectTimeoutMillis, readTimeoutMillis, false, true);
        client.addFilter(retryClientFilter);
        return client;
    }

    @Provides
    @Singleton
    @Named("HubCluster")
    public static CuratorCluster buildHubCluster(CuratorFramework curator, HubProperties hubProperties) throws Exception {
        log.info("providing 'HubCluster' CuratorCluster");
        return new CuratorCluster(curator, "/HubCluster", true, false, new DecommissionCluster() {}, hubProperties);
    }

    @Provides
    @Singleton
    @Named("SpokeCluster")
    public static CuratorCluster buildSpokeCluster(CuratorFramework curator, SpokeDecommissionCluster spokeDecommissionCluster, HubProperties hubProperties) throws Exception {
        log.info("providing 'SpokeCluster' CuratorCluster");
        return new CuratorCluster(curator, "/SpokeCluster", false, true, spokeDecommissionCluster, hubProperties);
    }

    @Provides
    @Singleton
    public static WebSocketContainer buildWebSocketContainer() throws Exception {
        log.info("providing WebSocketContainer");
        ClientContainer container = new ClientContainer();
        container.start();
        return container;
    }

    @Provides
    @Singleton
    public static ObjectMapper provideObjectMapper() {
        log.info("providing ObjectMapper");
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    @Provides
    @Singleton
    @Named("WRITE")
    public FileSpokeStore provideWriteSpokeStore(HubProperties hubProperties) {
        log.info("providing 'WRITE' FileSpokeStore");
        String spokePath = hubProperties.getSpokePath(SpokeStore.WRITE);
        int spokeTtlMinutes = hubProperties.getSpokeTtlMinutes(SpokeStore.WRITE);
        return new FileSpokeStore(spokePath, spokeTtlMinutes);
    }

    @Provides
    @Singleton
    @Named("READ")
    public FileSpokeStore provideReadSpokeStore(HubProperties hubProperties) {
        log.info("providing 'READ' FileSpokeStore");
        String spokePath = hubProperties.getSpokePath(SpokeStore.READ);
        int spokeTtlMinutes = hubProperties.getSpokeTtlMinutes(SpokeStore.READ);
        return new FileSpokeStore(spokePath, spokeTtlMinutes);
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        log.info("providing Gson");
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new HubDateTypeAdapter())
                .registerTypeAdapter(DateTime.class, new HubDateTimeTypeAdapter())
                .create();
    }

    @Provides
    @Singleton
    @Named("HubPort")
    public int provideHubPort(HubProperties hubProperties) {
        log.info("providing 'HubPort' int");
        return hubProperties.getProperty("http.bind_port", 8080);
    }

    @Provides
    @Singleton
    @Named("HubScheme")
    public String provideHubScheme(HubProperties hubProperties) {
        log.info("providing 'HubScheme' String");
        return hubProperties.isAppEncrypted() ? "https://" : "http://";
    }

}
