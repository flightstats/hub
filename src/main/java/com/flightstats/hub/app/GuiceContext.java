package com.flightstats.hub.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.rest.RetryClientFilter;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.HalLinksSerializer;
import com.flightstats.rest.Rfc3339DateSerializer;
import com.google.common.base.Strings;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.WebSocketContainer;
import javax.ws.rs.ext.ContextResolver;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.sun.jersey.api.core.PackagesResourceConfig.*;

public class GuiceContext {
    public static final String HAZELCAST_CONFIG_FILE = "hazelcast.conf.xml";
    private final static Logger logger = LoggerFactory.getLogger(GuiceContext.class);
    private static Properties properties = new Properties();

    public static HubGuiceServlet construct(
            final Properties properties, Module appModule) {
        GuiceContext.properties = properties;

        Map<String, String> jerseyProps = new HashMap<>();
        jerseyProps.put(PROPERTY_PACKAGES, "com.flightstats.hub.service");
        jerseyProps.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, GZIPContentEncodingFilter.class.getName());

        jerseyProps.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
        jerseyProps.put(FEATURE_CANONICALIZE_URI_PATH, "true");
        jerseyProps.put(PROPERTY_CONTAINER_REQUEST_FILTERS, GZIPContentEncodingFilter.class.getName() +
                ";" + RemoveSlashFilter.class.getName());

        JerseyServletModule jerseyModule = new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                Names.bindProperties(binder(), properties);
                ObjectMapper mapper = objectMapper();
                bind(ObjectMapper.class).toInstance(mapper);
                bind(ObjectMapperResolver.class).toInstance(new ObjectMapperResolver(mapper));
                bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
                serve("/*").with(GuiceContainer.class, jerseyProps);
            }
        };

        return new HubGuiceServlet(jerseyModule, appModule, new HubCommonModule());
    }

    public static class HubGuiceServlet extends GuiceServletContextListener {
        @NotNull
        private final Module[] modules;
        private Injector injector;

        public HubGuiceServlet(@NotNull Module... modules) {
            this.modules = modules;
        }

        @Override
        public synchronized Injector getInjector() {
            if (injector == null) {
                injector = Guice.createInjector(modules);
            }
            return injector;
        }
    }

    public static class HubCommonModule extends AbstractModule {
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

        @Named("ChannelConfigurationMap")
        @Singleton
        @Provides
        public static ConcurrentMap<String, ChannelConfiguration> buildChannelConfigurationMap(HazelcastInstance hazelcast) throws FileNotFoundException {
            return hazelcast.getMap("ChannelConfigurationMap");
        }

        @Singleton
        @Provides
        public static CuratorFramework buildCurator(@Named("app.name") String appName, @Named("app.environment") String environment,
                                                    @Named("zookeeper.connection") String zkConnection,
                                                    RetryPolicy retryPolicy, ZooKeeperState zooKeeperState) {
            logger.info("connecting to zookeeper(s) at " + zkConnection);
            FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider(zkConnection);
            CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().namespace(appName + "-" + environment)
                    .ensembleProvider(ensembleProvider)
                    .retryPolicy(retryPolicy).build();
            curatorFramework.getConnectionStateListenable().addListener(zooKeeperState.getStateListener());
            curatorFramework.start();

            try {
                Stat stat = curatorFramework.checkExists().forPath("/startup");
            } catch (Exception e) {
                logger.warn("unable to access zookeeper");
                throw new RuntimeException("unable to access zookeeper");
            }
            return curatorFramework;
        }

        @Singleton
        @Provides
        public static RetryPolicy buildRetryPolicy() {
            Integer baseSleepTimeMs = Integer.valueOf(properties.getProperty("zookeeper.baseSleepTimeMs", "10"));
            Integer maxSleepTimeMs = Integer.valueOf(properties.getProperty("zookeeper.maxSleepTimeMs", "10000"));
            Integer maxRetries = Integer.valueOf(properties.getProperty("zookeeper.maxRetries", "20"));

            return new BoundedExponentialBackoffRetry(baseSleepTimeMs, maxSleepTimeMs, maxRetries);
        }

        @Singleton
        @Provides
        public static Client buildJerseyClient() {
            return create(true);
        }

        @Named("NoRedirects")
        @Singleton
        @Provides
        public static Client buildJerseyClientNoRedirects() {
            return create(false);
        }

        private static Client create(boolean followRedirects) {
            Integer connectTimeoutSeconds = Integer.valueOf(properties.getProperty("http.connect.timeout.seconds", "30"));
            Integer readTimeoutSeconds = Integer.valueOf(properties.getProperty("http.read.timeout.seconds", "120"));
            int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(connectTimeoutSeconds);
            int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(readTimeoutSeconds);

            Client client = Client.create();
            client.setConnectTimeout(connectTimeoutMillis);
            client.setReadTimeout(readTimeoutMillis);
            client.addFilter(new RetryClientFilter());
            client.addFilter(new com.sun.jersey.api.client.filter.GZIPContentEncodingFilter());
            client.setFollowRedirects(followRedirects);
            return client;
        }

        @Singleton
        @Provides
        public static WebSocketContainer buildWebSocketContainer() throws Exception {
            ClientContainer container = new ClientContainer();
            container.start();
            return container;
        }

        @Override
        protected void configure() {
        }
    }

    @javax.ws.rs.ext.Provider
    @Singleton
    static class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
        private final ObjectMapper objectMapper;

        public ObjectMapperResolver(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return objectMapper;
        }
    }

    private static ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
