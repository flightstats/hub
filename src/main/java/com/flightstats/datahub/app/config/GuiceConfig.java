package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.*;
import com.flightstats.datahub.dao.prototypes.InMemoryChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.serialize.JacksonHectorSerializer;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.service.eventing.JettyWebSocketServlet;
import com.flightstats.datahub.service.eventing.SubscriptionDispatcher;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import me.prettyprint.hector.api.Serializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GuiceConfig extends GuiceServletContextListener {

	public static final String DATAHUB_PROPERTIES_FILENAME = "datahub.properties";
	public static final String CASSANDRA_BACKING_STORE_TAG = "cassandra";
	public static final String BACKING_STORE_PROPERTY = "backing.store";
	public static final String MEMORY_BACKING_STORY_TAG = "memory";

	private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

	static {
		JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
		JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.datahub");
	}

	@Override
	protected Injector getInjector() {
		Properties properties = loadProperties();
		String backingStoreName = properties.getProperty(BACKING_STORE_PROPERTY);
		Module jerseyServletModule;
		if (backingStoreName == null || CASSANDRA_BACKING_STORE_TAG.equals(backingStoreName)) {
			jerseyServletModule = new CassandraBackedDataHubModule(properties);
		} else if (MEMORY_BACKING_STORY_TAG.equals(backingStoreName)) {
			jerseyServletModule = new MemoryBackedDataHubModule();
		} else {
			throw new IllegalStateException(String.format("Unknown backing store specified: %s", backingStoreName));
		}
		return Guice.createInjector(jerseyServletModule);
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

	private static class BaseDataHubModule extends JerseyServletModule {

		protected void configureBaseServlets() {
			bind(ChannelLockExecutor.class).in(Singleton.class);
			bind(SubscriptionDispatcher.class).in(Singleton.class);
			bind(SubscriptionRoster.class).in(Singleton.class);
			bind(JettyWebSocketServlet.class).in(Singleton.class);
			bind(DataHubKeyRenderer.class).in(Singleton.class);
			bind(DataHubKeyGenerator.class).in(Singleton.class);
			bind(new TypeLiteral<RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>>() {
			}).to(YearMonthDayRowKeyStrategy.class);
			serveRegex("/channel/\\w+/ws").with(JettyWebSocketServlet.class);
			serve("/*").with(GuiceContainer.class, JERSEY_PROPERTIES);
		}
	}

	private static class MemoryBackedDataHubModule extends BaseDataHubModule {
		@Override
		protected void configureServlets() {
			bind(ChannelDao.class).to(InMemoryChannelDao.class).in(Singleton.class);
			super.configureBaseServlets();
		}

	}

	private static class CassandraBackedDataHubModule extends BaseDataHubModule {

		private final ObjectMapper objectMapper = new DataHubObjectMapperFactory().build();
		private final JacksonHectorSerializer<ChannelConfiguration> jacksonHectorSerializer = new JacksonHectorSerializer<>(objectMapper, ChannelConfiguration.class);
		private final Properties properties;

		public CassandraBackedDataHubModule(Properties properties) {
			this.properties = properties;
		}

		@Override
		protected void configureServlets() {
			super.configureBaseServlets();
			Names.bindProperties(binder(), properties);
			bind(CassandraChannelDao.class).asEagerSingleton();
			bind(CassandraConnectorFactory.class).in(Singleton.class);
			bind(new TypeLiteral<Serializer<ChannelConfiguration>>() {
			}).toInstance(jacksonHectorSerializer);
			bind(ChannelDao.class).to(CassandraChannelDao.class).in(Singleton.class);
		}

		@Inject
		@Provides
		@Singleton
		public CassandraConnector buildCassandraConnector(CassandraConnectorFactory factory) {
			return factory.build();
		}

	}
}
