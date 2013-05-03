package com.flightstats.datahub.app;

import com.flightstats.datahub.app.config.EmptyServlet;
import com.flightstats.datahub.app.config.GuiceConfig;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.DispatcherType;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Properties;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class DataHubMain {

	private static final String DEFAULT_HOST = "0.0.0.0";
	private static final int DEFAULT_PORT = 8080;
	private static final int DEFAULT_IDLE_TIMEOUT = 30000;
	public static final String DATAHUB_PROPERTIES_FILENAME = "datahub.properties";

	public static void main(String[] args) throws Exception {
		Server server = new Server();

		HttpConfiguration httpConfig = new HttpConfiguration();

		ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);

		ServerConnector serverConnector = new ServerConnector(server, connectionFactory);

		//TODO: Don't hard code these here.
		serverConnector.setHost(DEFAULT_HOST);
		serverConnector.setPort(DEFAULT_PORT);
		serverConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);

		server.setConnectors(new Connector[]{serverConnector});

		ServletContextHandler rootContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

		Properties properties = loadProperties(args);
		GuiceConfig guiceConfig = new GuiceConfig(properties);
		rootContextHandler.addEventListener(guiceConfig);
		rootContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		rootContextHandler.addServlet(EmptyServlet.class, "/*");

		server.start();
		server.join();
	}

	//TODO: This basically exists in the Users project as the PropertiesLoader.  We should eventually
	//consolidate this code and eliminate duplication.
	private static Properties loadProperties(String[] args) throws IOException {
		try (InputStream in = openInputStream(args)) {
			Properties properties = new Properties();
			properties.load(in);
			return properties;
		} catch (IOException e) {
			throw new RuntimeException("Error loading properties.", e);
		}
	}

	private static InputStream openInputStream(String[] args) throws FileNotFoundException {
		if (args.length == 0) {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(DATAHUB_PROPERTIES_FILENAME);
		}
		return new FileInputStream(args[0]);
	}

}
