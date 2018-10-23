package com.flightstats.hub.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.filter.CORSFilter;
import com.flightstats.hub.filter.MetricsRequestFilter;
import com.flightstats.hub.filter.StreamEncodingFilter;
import com.flightstats.hub.ws.WebSocketChannelEndpoint;
import com.flightstats.hub.ws.WebSocketDayEndpoint;
import com.flightstats.hub.ws.WebSocketHashEndpoint;
import com.flightstats.hub.ws.WebSocketHourEndpoint;
import com.flightstats.hub.ws.WebSocketMinuteEndpoint;
import com.flightstats.hub.ws.WebSocketSecondEndpoint;
import com.google.common.io.Resources;
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

import javax.websocket.server.ServerContainer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public class HubJettyServer {

    private final HubProperties hubProperties;
    private final MetricsRequestFilter metricsRequestFilter;

    private Server server;

    HubJettyServer(HubProperties hubProperties, MetricsRequestFilter metricsRequestFilter) {
        this.hubProperties = hubProperties;
        this.metricsRequestFilter = metricsRequestFilter;
    }

    private String getKeyStorePath() {
        String path = hubProperties.getProperty("app.keyStorePath", "/etc/ssl/") + HubHost.getLocalName() + ".jks";
        log.info("using key store path: {}", path);
        return path;
    }

    public void start() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new ObjectMapperResolver(HubModule.provideObjectMapper()));
        resourceConfig.register(JacksonJsonProvider.class);
        resourceConfig.registerClasses(
                CORSFilter.class,
                EncodingFilter.class,
                StreamEncodingFilter.class,
                GZipEncoder.class,
                DeflateEncoder.class);
        resourceConfig.packages("com.flightstats.hub");

        checkState(server == null, "Server has already been started");
        try {

            server = new Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
            SslContextFactory sslContextFactory = getSslContextFactory();
            if (null != sslContextFactory) {
                httpConfig.addCustomizer(new SecureRequestCustomizer());
            }
            ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
            ServerConnector serverConnector = new ServerConnector(server, sslContextFactory, connectionFactory);
            serverConnector.setHost(hubProperties.getProperty("http.bind_ip", "0.0.0.0"));
            serverConnector.setPort(HubHost.getLocalPort());
            serverConnector.setIdleTimeout(hubProperties.getProperty("http.idle_timeout", 30 * 1000));

            server.setConnectors(new Connector[]{serverConnector});

            JettyHttpContainer httpContainer = ContainerFactory.createContainer(JettyHttpContainer.class, resourceConfig);

            ServletContextHandler wsContext = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(wsContext);
            wsContainer.addEndpoint(WebSocketChannelEndpoint.class);
            wsContainer.addEndpoint(WebSocketDayEndpoint.class);
            wsContainer.addEndpoint(WebSocketHourEndpoint.class);
            wsContainer.addEndpoint(WebSocketMinuteEndpoint.class);
            wsContainer.addEndpoint(WebSocketSecondEndpoint.class);
            wsContainer.addEndpoint(WebSocketHashEndpoint.class);

            HttpAndWSHandler handler = new HttpAndWSHandler(httpContainer, wsContext, metricsRequestFilter);
            server.setHandler(handler);
            server.start();
        } catch (Exception e) {
            log.error("Exception in JettyServer: " + e.getMessage(), e);
            throw new RuntimeException("Failure in JettyServer: " + e.getMessage(), e);
        }
    }

    private SslContextFactory getSslContextFactory() throws IOException {
        SslContextFactory sslContextFactory = null;
        if (hubProperties.isAppEncrypted()) {
            log.info("starting hub with ssl!");
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getKeyStorePath());
            String keyStorePasswordPath = hubProperties.getProperty("app.keyStorePasswordPath", "/etc/ssl/key");
            URL passwordUrl = new File(keyStorePasswordPath).toURI().toURL();
            String password = Resources.readLines(passwordUrl, StandardCharsets.UTF_8).get(0);
            sslContextFactory.setKeyStorePassword(password);
        }
        return sslContextFactory;
    }

    void halt() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            log.warn("Exception while stopping JettyServer: " + e.getMessage(), e);
        }
    }
}
