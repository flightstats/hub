package com.flightstats.datahub.app.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import java.util.HashMap;
import java.util.Map;

public class GuiceConfig extends GuiceServletContextListener {

    private final static Map<String, String> JERSEY_PROPERTIES = new HashMap<>();

    static {
        JERSEY_PROPERTIES.put(ServletContainer.RESOURCE_CONFIG_CLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
        JERSEY_PROPERTIES.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");
        JERSEY_PROPERTIES.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.flightstats.datahub");
    }

    @Override
    protected Injector getInjector() {
        JerseyServletModule jerseyServletModule = new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                //                bind(UsersResource.class);
                //                bind(UsersDao.class).to(FakeDao.class).in(Singleton.class);
                serve("/*").with(GuiceContainer.class, JERSEY_PROPERTIES);
            }
        };
        return Guice.createInjector(jerseyServletModule);
    }
}
