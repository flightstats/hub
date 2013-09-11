package com.flightstats.cryptoproxy.app.config;

import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.app.config.metrics.PerChannelTimedMethodDispatchAdapter;
import com.flightstats.jerseyguice.Bindings;
import com.flightstats.jerseyguice.JerseyServletModuleBuilder;
import com.flightstats.jerseyguice.metrics.GraphiteConfig;
import com.flightstats.jerseyguice.metrics.GraphiteConfigImpl;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class GuiceContextListenerFactory {
    public static GuiceServletContextListener construct(
            @NotNull final Properties properties) throws ConstraintException {
        GraphiteConfig graphiteConfig = new GraphiteConfigImpl(properties);

        JerseyServletModule jerseyModule = new JerseyServletModuleBuilder()
                .withJerseyPackage("com.flightstats.cryptoproxy")
                .withJerseryProperty(JSONConfiguration.FEATURE_POJO_MAPPING, "true")
                .withNamedProperties(properties)
                .withGraphiteConfig(graphiteConfig)
                .withObjectMapper(DataHubObjectMapperFactory.construct())
                .withBindings(new CryptoBindings())
                .build();

        return new CryptoProxyGuiceServletContextListener(jerseyModule, new CryptoProxyModule());
    }

    public static class CryptoBindings implements Bindings {

        @Override
        public void bind(Binder binder) {
            binder.bind(PerChannelTimedMethodDispatchAdapter.class).asEagerSingleton();
        }
    }


    public static class CryptoProxyGuiceServletContextListener extends GuiceServletContextListener {
        @NotNull
        private final Module[] modules;

        public CryptoProxyGuiceServletContextListener(@NotNull Module... modules) {
            this.modules = modules;
        }

        @Override
        protected Injector getInjector() {
            return Guice.createInjector(modules);
        }
    }

    public static class CryptoProxyModule extends AbstractModule {
        @Override
        protected void configure() {
        }

        @Inject
        @Provides
        public static Client buildRestClient(@Named("restclient.connect.timeout.seconds") int connectTimeoutSec, @Named("restclient.read.timeout.seconds") int readTimeoutSec) {
            Client client = Client.create();
            client.setConnectTimeout(connectTimeoutSec * 1000);
            client.setReadTimeout(readTimeoutSec * 1000);
            return client;
        }
    }
}
