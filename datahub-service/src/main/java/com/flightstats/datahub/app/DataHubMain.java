package com.flightstats.datahub.app;

import com.conducivetech.services.common.util.PropertyConfiguration;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.servlet.GuiceServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class DataHubMain {

    private static final Logger logger = LoggerFactory.getLogger(DataHubMain.class);

    public static void main(String[] args) throws Exception {

        Properties properties = loadProperties(args);
        final JettyConfig jettyConfig = new JettyConfigImpl(properties);
        final GuiceServletContextListener guice = GuiceContextListenerFactory.construct(properties);
        new JettyServer(jettyConfig, guice).start();
    }

    private static Properties loadProperties(String[] args) throws IOException {
        if (args.length > 0) {
            return PropertyConfiguration.loadProperties(new File(args[0]), true, logger);
        }
        URL resource = DataHubMain.class.getResource("/default.properties");
        return PropertyConfiguration.loadProperties(resource, true, logger);
    }
}
