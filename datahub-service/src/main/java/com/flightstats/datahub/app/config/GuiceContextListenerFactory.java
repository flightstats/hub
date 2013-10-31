package com.flightstats.datahub.app.config;

import com.codahale.metrics.MetricRegistry;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.metrics.PerChannelTimedMethodDispatchAdapter;
import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.flightstats.datahub.cluster.HazelcastChannelLockFactory;
import com.flightstats.datahub.cluster.HazelcastClusterKeyGenerator;
import com.flightstats.datahub.dao.ChannelHourRowKeyStrategy;
import com.flightstats.datahub.dao.RowKeyStrategy;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.service.DataHubHealthCheck;
import com.flightstats.datahub.service.DataHubSweeper;
import com.flightstats.datahub.service.eventing.JettyWebSocketServlet;
import com.flightstats.datahub.service.eventing.MetricsCustomWebSocketCreator;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.flightstats.datahub.service.eventing.WebSocketChannelNameExtractor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.jerseyguice.Bindings;
import com.flightstats.jerseyguice.JerseyServletModuleBuilder;
import com.flightstats.jerseyguice.metrics.GraphiteConfig;
import com.flightstats.jerseyguice.metrics.GraphiteConfigImpl;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.util.Modules;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class GuiceContextListenerFactory {
    public static final String BACKING_STORE_PROPERTY = "backing.store";
    public static final String CASSANDRA_BACKING_STORE_TAG = "cassandra";
    public static final String MEMORY_BACKING_STORY_TAG = "memory";
    public static final String HAZELCAST_CONFIG_FILE = "hazelcast.conf.xml";

    public static GuiceServletContextListener construct(
            @NotNull final Properties properties) throws ConstraintException {
        GraphiteConfig graphiteConfig = new GraphiteConfigImpl(properties);

        Module module = getMaxPaloadSizeModule(properties);

        JerseyServletModule jerseyModule = new JerseyServletModuleBuilder()
                .withJerseyPackage("com.flightstats.datahub")
                .withContainerResponseFilters(GZIPContentEncodingFilter.class)
                .withJerseryProperty(JSONConfiguration.FEATURE_POJO_MAPPING, "true")
                .withJerseryProperty(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, "true")
                .withContainerRequestFilters(GZIPContentEncodingFilter.class, RemoveSlashFilter.class)
                .withNamedProperties(properties)
                .withGraphiteConfig(graphiteConfig)
                .withObjectMapper(DataHubObjectMapperFactory.construct())
                .withBindings(new DataHubBindings())
                .withHealthCheckClass(DataHubHealthCheck.class)
                .withRegexServe(WebSocketChannelNameExtractor.WEBSOCKET_URL_REGEX, JettyWebSocketServlet.class)
                .withModules(Arrays.asList(module))
                .build();

        return new DataHubGuiceServletContextListener(jerseyModule, createDataStoreModule(properties), new DatahubCommonModule());
    }

    private static Module getMaxPaloadSizeModule(Properties properties) {
        final int maxPayloadSizeBytes;
        Logger logger = LoggerFactory.getLogger(GuiceContextListenerFactory.class);
        String maxPayloadSizeMB = properties.getProperty("service.maxPayloadSizeMB");
        if (maxPayloadSizeMB == null) {
            logger.info("MAX_PAYLOAD_SIZE not specified, setting to the default 10MB");
            maxPayloadSizeBytes = 1024 * 10;
        } else {
            try {
                int mb = Integer.parseInt(maxPayloadSizeMB);
                maxPayloadSizeBytes = 1024 * mb;
                logger.info("Setting MAX_PAYLOAD_SIZE to " + maxPayloadSizeMB + "MB");
            } catch (NumberFormatException e) {
                throw new RuntimeException("Unable to parse 'service.maxPayloadSizeMB", e);
            }
        }

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(Integer.class).annotatedWith(Names.named("maxPayloadSizeBytes")).toInstance(maxPayloadSizeBytes);
            }
        };
    }

    public static class DataHubBindings implements Bindings {

        @Override
        public void bind(Binder binder) {
            binder.bind(MetricRegistry.class).in(Singleton.class);
            binder.bind(ChannelLockExecutor.class).asEagerSingleton();
            binder.bind(SubscriptionRoster.class).in(Singleton.class);
            binder.bind(DataHubKeyRenderer.class).in(Singleton.class);
            binder.bind(DataHubKeyGenerator.class).to(HazelcastClusterKeyGenerator.class).in(Singleton.class);
            binder.bind(ChannelLockFactory.class).to(HazelcastChannelLockFactory.class).in(Singleton.class);
            binder.bind(PerChannelTimedMethodDispatchAdapter.class).asEagerSingleton();
            binder.bind(WebSocketCreator.class).to(MetricsCustomWebSocketCreator.class).in(Singleton.class);
            binder.bind(new TypeLiteral<RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>>() {
            }).to(ChannelHourRowKeyStrategy.class);
            binder.bind(DataHubSweeper.class).asEagerSingleton();
            binder.bind(JettyWebSocketServlet.class).in(Singleton.class);
        }
    }

    public static class DataHubGuiceServletContextListener extends GuiceServletContextListener {
        @NotNull
        private final Module[] modules;

        public DataHubGuiceServletContextListener(@NotNull Module... modules) {
            this.modules = modules;
        }

        @Override
        protected Injector getInjector() {
            return Guice.createInjector(modules);
        }
    }

    private static Module createDataStoreModule(Properties properties) {
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

    public static class DatahubCommonModule extends AbstractModule {
        @Singleton
        @Provides
        public static HazelcastInstance buildHazelcast(@Named(HAZELCAST_CONFIG_FILE) String hazelcastConfigFile) throws FileNotFoundException {
            Config config;
            if (Strings.isNullOrEmpty(hazelcastConfigFile)) {
                config = new ClasspathXmlConfig("hazelcast.conf.xml");
            } else {
                config = new FileSystemXmlConfig(hazelcastConfigFile);
            }
            return Hazelcast.newHazelcastInstance(config);
        }

        @Named("LastUpdatePerChannelMap")
        @Singleton
        @Provides
        public static ConcurrentMap<String, DataHubKey> buildLastUpdatePerChannelMap(HazelcastInstance hazelcast) throws FileNotFoundException {
            return hazelcast.getMap("LAST_CHANNEL_UPDATE");
        }

        @Named("KnownChannelCache")
        @Singleton
        @Provides
        public static Cache<String, Boolean> buildKnownChannelCache() throws FileNotFoundException {
            return CacheBuilder.newBuilder().maximumSize(1000)
                               .expireAfterAccess(15L, TimeUnit.MINUTES)
                               .build();
        }

        @Override
        protected void configure() {
        }
    }
}
