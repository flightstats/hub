package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.PropertiesLoaderOverride;
import com.flightstats.hub.system.config.GuiceModule;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Properties;

@Slf4j
@Builder
public class SingletonTestInjectionExtension implements TestInstancePostProcessor {
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    private Properties getProperties(Object testInstance) {
        Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
        log.info(properties.toString());
        return new PropertiesLoaderOverride(properties).get(testInstance);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        Injector injector = Guice.createInjector(new GuiceModule(getProperties(testInstance)));
        log.info("@@@@@@@@@@@@@@@@@@@@@ injecting properties ");
        injector.injectMembers(testInstance);
    }
}
