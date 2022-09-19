package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.GuiceModule;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Properties;

@Builder
public class SingletonTestInjectionExtension implements TestInstancePostProcessor {
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    private Properties getProperties(Object testInstance) {
        return new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        Injector injector = Guice.createInjector(new GuiceModule(getProperties(testInstance)));
        injector.injectMembers(testInstance);
    }
}
