package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.*;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.serialize.JacksonHectorSerializer;
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

    private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

    static {
        JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
        JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
        JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.datahub");
    }

    @Override
    protected Injector getInjector() {
        Module jerseyServletModule = new DataHubModule();
        return Guice.createInjector(jerseyServletModule);
    }

    private static class DataHubModule extends JerseyServletModule {

        private final ObjectMapper objectMapper = new DataHubObjectMapperFactory().build();
        private final JacksonHectorSerializer<ChannelConfiguration> jacksonHectorSerializer = new JacksonHectorSerializer<>(objectMapper,
                ChannelConfiguration.class);

        @Override
        protected void configureServlets() {
            Properties properties = loadProperties();
            Names.bindProperties(binder(), properties);
            bind(CassandraChannelDao.class).asEagerSingleton();
            bind(CassandraConnectorFactory.class).in(Singleton.class);
            bind(DataHubKeyRenderer.class).in(Singleton.class);
            bind(DataHubKeyGenerator.class).in(Singleton.class);
            bind(new TypeLiteral<Serializer<ChannelConfiguration>>() {
            }).toInstance(jacksonHectorSerializer);
            bind(new TypeLiteral<RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>>() {
            }).to(YearMonthDayRowKeyStrategy.class);
            bind(ChannelDao.class).to(CassandraChannelDao.class).in(Singleton.class);
            serve("/*").with(GuiceContainer.class, JERSEY_PROPERTIES);
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

        @Inject
        @Provides
        @Singleton
        public CassandraConnector buildCassandraConnector(CassandraConnectorFactory factory) {
            return factory.build();
        }

    }
}
