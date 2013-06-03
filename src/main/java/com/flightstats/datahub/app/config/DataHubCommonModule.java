package com.flightstats.datahub.app.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.flightstats.datahub.dao.RowKeyStrategy;
import com.flightstats.datahub.dao.YearMonthDayRowKeyStrategy;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.service.eventing.JettyWebSocketServlet;
import com.flightstats.datahub.service.eventing.SubscriptionDispatcher;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import java.util.HashMap;
import java.util.Map;

import static com.flightstats.datahub.service.eventing.WebSocketChannelNameExtractor.WEBSOCKET_URL_REGEX;

class DataHubCommonModule extends JerseyServletModule {
	private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

	static {
		JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
		JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.datahub");
	}

	@Override
	protected void configureServlets() {
		bindCommonBeans();
		startUpServlets();
	}

	private void bindCommonBeans() {
		bind(MetricRegistry.class).in(Singleton.class);
		bind(GraphiteReporterConfiguration.class).asEagerSingleton();
		bind(ChannelLockExecutor.class).in(Singleton.class);
		bind(SubscriptionDispatcher.class).in(Singleton.class);
		bind(SubscriptionRoster.class).in(Singleton.class);
		bind(DataHubKeyRenderer.class).in(Singleton.class);
		bind(DataHubKeyGenerator.class).in(Singleton.class);
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
}
