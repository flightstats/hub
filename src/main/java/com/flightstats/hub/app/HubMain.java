package com.flightstats.hub.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.config.SystemProperties;
import com.flightstats.hub.config.ZookeeperProperties;
import com.flightstats.hub.config.binding.ClusterHubBindings;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.config.binding.PropertiesBinding;
import com.flightstats.hub.config.binding.SingleHubBindings;
import com.flightstats.hub.dao.aws.S3WriteQueueLifecycle;
import com.flightstats.hub.filter.CORSFilter;
import com.flightstats.hub.filter.StreamEncodingFilter;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.spoke.SpokeTtlEnforcerService;
import com.flightstats.hub.ws.WebSocketChannelEndpoint;
import com.flightstats.hub.ws.WebSocketDayEndpoint;
import com.flightstats.hub.ws.WebSocketHashEndpoint;
import com.flightstats.hub.ws.WebSocketHourEndpoint;
import com.flightstats.hub.ws.WebSocketMinuteEndpoint;
import com.flightstats.hub.ws.WebSocketSecondEndpoint;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.joda.time.DateTime;

import javax.websocket.server.ServerContainer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class HubMain {

    private static DateTime startTime = new DateTime();
    private final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());;
    private final SystemProperties systemProperties = new SystemProperties(PropertiesLoader.getInstance());;
    private final ZookeeperProperties zookeeperProperties = new ZookeeperProperties(PropertiesLoader.getInstance());;
    private final StorageBackend storageBackend = StorageBackend.valueOf(appProperties.getHubType());;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'");
        }

        PropertiesLoader.getInstance().load(args[0]);
        new HubMain().run();
    }

    public static DateTime getStartTime() {
        return startTime;
    }

    public void run() throws Exception {

        Security.setProperty("networkaddress.cache.ttl", "60");
        startZookeeperIfSingle();
        Server server = startServer();

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Jetty Server shutting down...");
            latch.countDown();
        }));
        latch.await();

        log.warn("calling shutdown");
        HubProvider.getInstance(ShutdownManager.class).shutdown(true);

        server.stop();
    }

    private void startZookeeperIfSingle() {
        new Thread(() -> {
            String zkConfigFile = zookeeperProperties.getZookeeperRunMode();
            if ("singleNode".equals(zkConfigFile)) {
                log.warn("using single node zookeeper");
                ZookeeperMain.start();
            }
        }).start();
    }

    @VisibleForTesting
    public Server startServer() throws Exception {

        List<AbstractModule> guiceModules = buildGuiceModules();
        Injector injector = Guice.createInjector(guiceModules);

        registerServices(getBeforeHealthCheckServices(injector), HubServices.TYPE.BEFORE_HEALTH_CHECK);
        registerServices(getAfterHealthCheckServices(injector), HubServices.TYPE.AFTER_HEALTHY_START);

        HubProvider.setInjector(injector);
        HubServices.start(HubServices.TYPE.BEFORE_HEALTH_CHECK);

        // build Jetty server
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        SslContextFactory sslContextFactory = getSslContextFactory();
        if (null != sslContextFactory) {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
        }
        ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
        ServerConnector serverConnector = new ServerConnector(server, sslContextFactory, connectionFactory);
        serverConnector.setHost(systemProperties.getHttpBindIp());
        serverConnector.setPort(HubHost.getLocalPort());
        serverConnector.setIdleTimeout(systemProperties.getHttpIdleTimeInMillis());
        server.setConnectors(new Connector[]{serverConnector});

        // build Jersey HTTP context
        ResourceConfig resourceConfig = buildResourceConfig(injector);
        JettyHttpContainer httpContainer = ContainerFactory.createContainer(JettyHttpContainer.class, resourceConfig);

        // build Jetty WebSocket context
        ServletContextHandler wsContext = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        wsContext.setContextPath("/");
        ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(wsContext);
        wsContainer.addEndpoint(WebSocketChannelEndpoint.class);
        wsContainer.addEndpoint(WebSocketDayEndpoint.class);
        wsContainer.addEndpoint(WebSocketHourEndpoint.class);
        wsContainer.addEndpoint(WebSocketMinuteEndpoint.class);
        wsContainer.addEndpoint(WebSocketSecondEndpoint.class);
        wsContainer.addEndpoint(WebSocketHashEndpoint.class);

        // use handler collection to choose the proper context
        HttpAndWSHandler handler = new HttpAndWSHandler();
        handler.addHttpHandler(httpContainer);
        handler.addWSHandler(wsContext);
        server.setHandler(handler);

        // start everything up
        server.start();

        log.info("Hub server has been started.");
        HubServices.start(HubServices.TYPE.PERFORM_HEALTH_CHECK);

        log.info("completed initial post start");
        HubServices.start(HubServices.TYPE.AFTER_HEALTHY_START);

        return server;
    }

    private List<Service> getBeforeHealthCheckServices(Injector injector) {

        final List<Service> services = Stream.of(
                InfluxdbReporterLifecycle.class,
                StatsDReporterLifecycle.class)
                .map(injector::getInstance)
                .collect(Collectors.toList());

        if (storageBackend == StorageBackend.aws) {
            services.add(injector.getInstance(S3WriteQueueLifecycle.class));
            if (spokeProperties.isTtlEnforced()) {
                services.add(new SpokeTtlEnforcerService(SpokeStore.WRITE, injector.getInstance(SpokeTtlEnforcer.class)));
                services.add(new SpokeTtlEnforcerService(SpokeStore.READ, injector.getInstance(SpokeTtlEnforcer.class)));
            }
        }

        return services;
    }

    private List<Service> getAfterHealthCheckServices(Injector injector) {
        if (storageBackend != StorageBackend.aws) {
            return Collections.singletonList(injector.getInstance(CustomMetricsLifecycle.class));
        }
        return Stream.of(
                CustomMetricsLifecycle.class,
                PeriodicMetricEmitterLifecycle.class)
                .map(injector::getInstance)
                .collect(Collectors.toList());
    }

    private void registerServices(List<Service> services, HubServices.TYPE type) {
        services.forEach(service -> HubServices.register(service, type));
    }

    private List<AbstractModule> buildGuiceModules() {
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new PropertiesBinding());
        modules.add(new HubBindings());

        log.info("starting with hub.type {}", storageBackend);

        modules.add(getGuiceModuleForHubType(storageBackend.toString()));
        return modules;
    }

    private AbstractModule getGuiceModuleForHubType(String type) {
        switch (type) {
            case "aws":
                return new ClusterHubBindings();
            case "nas":
            case "test":
                return new SingleHubBindings();
            default:
                throw new RuntimeException("unsupported hub.type " + type);
        }
    }

    private ResourceConfig buildResourceConfig(Injector injector) {
        ResourceConfig config = new ResourceConfig();
        config.register(new GuiceToHK2Adapter(injector));
        config.register(new ObjectMapperResolver(HubBindings.objectMapper()));
        config.register(JacksonJsonProvider.class);
        config.registerClasses(
                CORSFilter.class,
                EncodingFilter.class,
                StreamEncodingFilter.class,
                GZipEncoder.class,
                DeflateEncoder.class
        );
        config.packages("com.flightstats.hub");
        return config;
    }

    private SslContextFactory getSslContextFactory() throws IOException {
        SslContextFactory sslContextFactory = null;
        if (appProperties.isAppEncrypted()) {
            log.info("starting hub with ssl!");
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getKeyStorePath());
            String keyStorePasswordPath = appProperties.getKeyStorePasswordPath();
            URL passwordUrl = new File(keyStorePasswordPath).toURI().toURL();
            String password = Resources.readLines(passwordUrl, StandardCharsets.UTF_8).get(0);
            sslContextFactory.setKeyStorePassword(password);
        }
        return sslContextFactory;
    }

    private String getKeyStorePath() {
        final String path = appProperties.getKeyStorePath() + HubHost.getLocalName() + ".jks";
        log.info("using key store path: {}", path);
        return path;
    }

}
