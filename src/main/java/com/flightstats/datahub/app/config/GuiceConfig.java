package com.flightstats.datahub.app.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GuiceConfig extends GuiceServletContextListener {

	public static final String DATAHUB_PROPERTIES_FILENAME = "datahub.properties";
	public static final String BACKING_STORE_PROPERTY = "backing.store";
	public static final String CASSANDRA_BACKING_STORE_TAG = "cassandra";
	public static final String MEMORY_BACKING_STORY_TAG = "memory";

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(createModule());
	}

	private BaseDataHubModule createModule() {
		Properties properties = loadProperties();
		String backingStoreName = properties.getProperty(BACKING_STORE_PROPERTY, MEMORY_BACKING_STORY_TAG);
		switch (backingStoreName) {
			case CASSANDRA_BACKING_STORE_TAG:
				return new CassandraBackedDataHubModule(properties);
			case MEMORY_BACKING_STORY_TAG:
				return new MemoryBackedDataHubModule();
			default:
				throw new IllegalStateException(String.format("Unknown backing store specified: %s", backingStoreName));
		}
	}

	private Properties loadProperties() {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(DATAHUB_PROPERTIES_FILENAME);
		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Error loading properties.", e);
		}
		return properties;
	}

}
