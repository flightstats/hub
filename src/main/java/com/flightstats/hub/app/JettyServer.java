package com.flightstats.hub.app;

import com.flightstats.hub.ws.ChannelWSEndpoint;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.websocket.server.ServerContainer;
import java.util.EnumSet;
import java.util.EventListener;

import static com.google.common.base.Preconditions.checkState;

public class JettyServer {

    private final static Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private final EventListener guice;
    private Server server;

    public JettyServer(EventListener guice) {
        this.guice = guice;
    }

    public void start() {
        checkState(server == null, "Server has already been started");
        try {
            server = new Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
            ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);

            ServerConnector serverConnector = new ServerConnector(server, connectionFactory);
            serverConnector.setHost(HubProperties.getProperty("http.bind_ip", "0.0.0.0"));
            serverConnector.setPort(HubProperties.getProperty("http.bind_port", 8080));
            serverConnector.setIdleTimeout(HubProperties.getProperty("http.idle_timeout", 30 * 1000));

            server.setConnectors(new Connector[]{serverConnector});

            ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(context);
            wsContainer.addEndpoint(ChannelWSEndpoint.class);

            context.addEventListener(guice);
            context.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

            server.start();
        } catch (Exception e) {
            logger.error("Exception in JettyServer: " + e.getMessage(), e);
            throw new RuntimeException("Failure in JettyServer: " + e.getMessage(), e);
        }
    }

    public void halt() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            logger.warn("Exception while stopping JettyServer: " + e.getMessage(), e);
        }
    }
}
