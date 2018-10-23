package com.flightstats.hub.test;

import com.flightstats.hub.app.HubApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestMain {

    public static Injector start() throws Exception {
        Properties properties = loadProperties();
        Injector injector = Guice.createInjector(new TestModule(properties));
        injector.getInstance(HubApplication.class).run();
        return injector;
    }

    private static Properties loadProperties() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("hub.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }

}
