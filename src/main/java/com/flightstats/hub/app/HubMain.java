package com.flightstats.hub.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.filter.CORSFilter;
import com.flightstats.hub.filter.StreamEncodingFilter;
import com.flightstats.hub.ws.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.*;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class HubMain {

    private static final DateTime startTime = new DateTime();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'");
        }
        HubProperties.loadProperties(args[0]);
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
            String zkConfigFile = HubProperties.getProperty("runSingleZookeeperInternally", "");
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
        serverConnector.setHost(HubProperties.getProperty("http.bind_ip", "0.0.0.0"));
        serverConnector.setPort(HubHost.getLocalPort());
        serverConnector.setIdleTimeout(HubProperties.getProperty("http.idle_timeout", 30 * 1000));
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

    private List<AbstractModule> buildGuiceModules() {
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new HubBindings());

        String hubType = HubProperties.getProperty("hub.type", "aws");
        log.info("starting with hub.type {}", hubType);

        modules.add(getGuiceModuleForHubType(hubType));
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
        if (HubProperties.isAppEncrypted()) {
            log.info("starting hub with ssl!");
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getKeyStorePath());
            String keyStorePasswordPath = HubProperties.getProperty("app.keyStorePasswordPath", "/etc/ssl/key");
            URL passwordUrl = new File(keyStorePasswordPath).toURI().toURL();
            String password = Resources.readLines(passwordUrl, StandardCharsets.UTF_8).get(0);
            sslContextFactory.setKeyStorePassword(password);
        }
        return sslContextFactory;
    }

    private String getKeyStorePath() {
        String path = HubProperties.getProperty("app.keyStorePath", "/etc/ssl/") + HubHost.getLocalName() + ".jks";
        log.info("using key store path: {}", path);
        return path;
    }

}
