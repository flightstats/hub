package com.flightstats.cryptoproxy.app.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.flightstats.cryptoproxy.service.RestClient;
import com.flightstats.datahub.app.config.metrics.PerChannelTimedMethodDispatchAdapter;
import com.flightstats.cryptoproxy.app.config.metrics.GraphiteConfiguration;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class CryptoProxyCommonModule extends JerseyServletModule {
    private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

    static {
        JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
        JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
        JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.cryptoproxy");
    }

    private final Properties properties;

    CryptoProxyCommonModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configureServlets() {
        bindCommonBeans();
        startUpServlets();
    }

    private void bindCommonBeans() {
        Names.bindProperties(binder(), properties);
        bind(MetricRegistry.class).in(Singleton.class);
        bind(GraphiteConfiguration.class).asEagerSingleton();
        bind(PerChannelTimedMethodDispatchAdapter.class).asEagerSingleton();

    }

    private void startUpServlets() {
        serve("/*").with(GuiceContainer.class, JERSEY_PROPERTIES);
    }

    @Inject
    @Provides
    @Singleton
    public InstrumentedResourceMethodDispatchAdapter buildMetricsAdapter(MetricRegistry registry) {
        return new InstrumentedResourceMethodDispatchAdapter(registry);
    }

    @Inject
    @Provides
    public Client buildRestClient(@Named("restclient.connect.timeout.seconds") int connectTimeoutSec, @Named("restclient.read.timeout.seconds") int readTimeoutSec) {
        Client client = Client.create();
        client.setConnectTimeout(connectTimeoutSec * 1000);
        client.setReadTimeout(readTimeoutSec * 1000);
        return client;
    }
}
