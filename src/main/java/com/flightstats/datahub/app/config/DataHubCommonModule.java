package com.flightstats.datahub.app.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.flightstats.datahub.app.config.metrics.GraphiteConfiguration;
import com.flightstats.datahub.app.config.metrics.PerChannelTimedMethodDispatchAdapter;
import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.flightstats.datahub.cluster.HazelcastChannelLockFactory;
import com.flightstats.datahub.cluster.HazelcastClusterKeyGenerator;
import com.flightstats.datahub.cluster.HazelcastSubscriptionRoster;
import com.flightstats.datahub.dao.RowKeyStrategy;
import com.flightstats.datahub.dao.YearMonthDayRowKeyStrategy;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.service.eventing.*;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.flightstats.datahub.service.eventing.WebSocketChannelNameExtractor.WEBSOCKET_URL_REGEX;

class DataHubCommonModule extends JerseyServletModule {
	private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

	public static final String HAZELCAST_CONFIG_FILE = "hazelcast.conf.xml";

	static {
		JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
		JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.datahub");
	}

	private final Properties properties;

	DataHubCommonModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configureServlets() {
		bindCommonBeans();
		startUpServlets();
	}

	private void bindCommonBeans() {
		Names.bindProperties( binder(), properties );
		bind(MetricRegistry.class).in( Singleton.class );
		bind(GraphiteConfiguration.class).asEagerSingleton();
		bind(ChannelLockExecutor.class).asEagerSingleton();
		bind(SubscriptionRoster.class).to(HazelcastSubscriptionRoster.class).in(Singleton.class);
		bind(DataHubKeyRenderer.class).in(Singleton.class);
		bind(DataHubKeyGenerator.class).to(HazelcastClusterKeyGenerator.class).in(Singleton.class);
		bind(ChannelLockFactory.class).to(HazelcastChannelLockFactory.class).in(Singleton.class);
		bind(PerChannelTimedMethodDispatchAdapter.class).asEagerSingleton();
		bind(WebSocketCreator.class).to(MetricsCustomWebSocketCreator.class).in(Singleton.class);
		bind(new TypeLiteral<RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>>() {
		}).to(YearMonthDayRowKeyStrategy.class);
	}

	private void startUpServlets() {
		bind(JettyWebSocketServlet.class).in(Singleton.class);
		serveRegex(WEBSOCKET_URL_REGEX).with(JettyWebSocketServlet.class);
		serve("/*").with(GuiceContainer.class, JERSEY_PROPERTIES);
	}

	@Inject
	@Provides
	@Singleton
	public InstrumentedResourceMethodDispatchAdapter buildMetricsAdapter(MetricRegistry registry) {
		return new InstrumentedResourceMethodDispatchAdapter(registry);
	}

	@Provides
	@Singleton
	public HazelcastInstance buildHazelcast() throws FileNotFoundException {
		Config config;
		if (properties.contains(HAZELCAST_CONFIG_FILE)) {
			config = new FileSystemXmlConfig( properties.getProperty( HAZELCAST_CONFIG_FILE ) );
		} else {
			config = new ClasspathXmlConfig("hazelcast.conf.xml");
		}
		return Hazelcast.newHazelcastInstance(config);
	}
}
