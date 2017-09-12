package com.flightstats.hub.app;

import com.flightstats.hub.ws.*;
import com.google.common.io.Resources;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.server.ServerContainer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkState;

public class HubJettyServer {

    private final static Logger logger = LoggerFactory.getLogger(HubJettyServer.class);

    private Server server;

    private static String getKeyStorePath() throws UnknownHostException {
        String path = HubProperties.getProperty("app.keyStorePath", "/etc/ssl/") + HubHost.getLocalName() + ".jks";
        logger.info("using key store path: {}", path);
        return path;
    }

    public void start(ResourceConfig config) {
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
            serverConnector.setHost(HubProperties.getProperty("http.bind_ip", "0.0.0.0"));
            serverConnector.setPort(HubHost.getLocalPort());
            serverConnector.setIdleTimeout(HubProperties.getProperty("http.idle_timeout", 30 * 1000));

            server.setConnectors(new Connector[]{serverConnector});

            HttpAndWSHandler handler = new HttpAndWSHandler();
            handler.addHttpHandler(ContainerFactory.createContainer(JettyHttpContainer.class, config));

            ServletContextHandler wsContext = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(wsContext);
            wsContainer.addEndpoint(WebSocketChannelEndpoint.class);
            wsContainer.addEndpoint(WebSocketDayEndpoint.class);
            wsContainer.addEndpoint(WebSocketHourEndpoint.class);
            wsContainer.addEndpoint(WebSocketMinuteEndpoint.class);
            wsContainer.addEndpoint(WebSocketSecondEndpoint.class);
            wsContainer.addEndpoint(WebSocketHashEndpoint.class);
            handler.addWSHandler(wsContext);

            server.setHandler(handler);
            server.start();
        } catch (Exception e) {
            logger.error("Exception in JettyServer: " + e.getMessage(), e);
            throw new RuntimeException("Failure in JettyServer: " + e.getMessage(), e);
        }
    }

    private SslContextFactory getSslContextFactory() throws IOException {
        SslContextFactory sslContextFactory = null;
        if (HubProperties.isAppEncrypted()) {
            logger.info("starting hub with ssl!");
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getKeyStorePath());
            String keyStorePasswordPath = HubProperties.getProperty("app.keyStorePasswordPath", "/etc/ssl/key");
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
            logger.warn("Exception while stopping JettyServer: " + e.getMessage(), e);
        }
    }
}
