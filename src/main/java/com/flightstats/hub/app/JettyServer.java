package com.flightstats.hub.app;

import com.conducivetech.services.common.util.Haltable;
import com.flightstats.jerseyguice.jetty.EmptyServlet;
import com.flightstats.jerseyguice.jetty.JettyServerConnectorFactory;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class JettyServer implements Haltable {

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

            String bindIp = HubProperties.getProperty("http.bind_ip", "0.0.0.0");
            int httpPort = HubProperties.getProperty("http.bind_port", 8080);
            JettyServerConnectorFactory connectorFactory = new JettyServerConnectorFactory(server, connectionFactory, httpPort);
            List<Connector> connectors = connectorFactory.build(bindIp, null);
            server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

            ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            root.addEventListener(guice);
            root.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            //todo - gfm - 1/8/15 - do we need this?
            root.addServlet(EmptyServlet.class, "/*");

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
