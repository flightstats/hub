package com.flightstats.datahub.app.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;

import java.util.Properties;

public class GuiceConfig extends GuiceServletContextListener {

	public static final String BACKING_STORE_PROPERTY = "backing.store";
	public static final String CASSANDRA_BACKING_STORE_TAG = "cassandra";
	public static final String MEMORY_BACKING_STORY_TAG = "memory";
	private final Properties properties;

	public GuiceConfig(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new DataHubCommonModule(properties), createDataStoreModule());
	}

	private Module createDataStoreModule() {
		String backingStoreName = properties.getProperty(BACKING_STORE_PROPERTY, MEMORY_BACKING_STORY_TAG);
		switch (backingStoreName) {
			case CASSANDRA_BACKING_STORE_TAG:
				return new CassandraDataStoreModule(properties);
			case MEMORY_BACKING_STORY_TAG:
				return new MemoryBackedDataStoreModule();
			default:
				throw new IllegalStateException(String.format("Unknown backing store specified: %s", backingStoreName));
		}
	}
}
