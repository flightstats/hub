package com.flightstats.datahub.app;

import com.flightstats.datahub.app.config.EmptyServlet;
import com.flightstats.datahub.app.config.GuiceConfig;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class DataHubMain {

    public static void main(String[] args) throws Exception {
        Server server = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();

        ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);

        ServerConnector serverConnector = new ServerConnector(server, connectionFactory);

        //TODO: Don't hard code these here.
        serverConnector.setHost("0.0.0.0");
        serverConnector.setPort(8080);
        serverConnector.setIdleTimeout(30000);

        server.setConnectors(new Connector[]{serverConnector});

        ServletContextHandler rootContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        rootContextHandler.addEventListener(new GuiceConfig());
        rootContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        rootContextHandler.addServlet(EmptyServlet.class, "/*");
        //        rootContextHandler.setErrorHandler(xxx);

        server.start();
        server.join();
    }

}
