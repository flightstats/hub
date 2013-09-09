package com.flightstats.cryptoproxy.app.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import java.util.Properties;

public class GuiceConfig extends GuiceServletContextListener {

    private final Properties properties;

    public GuiceConfig(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new CryptoProxyCommonModule(properties));
    }
}
