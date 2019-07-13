package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.CustomPropertiesLoader;
import com.flightstats.hub.system.config.GuiceModule;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Properties;

@Builder
@AllArgsConstructor
public class SingletonTestInjectionExtension implements TestInstancePostProcessor {
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    private String hubDockerImage;

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
        properties = new CustomPropertiesLoader(properties)
                .withHubDockerImage(hubDockerImage)
                .getProperties();
        Injector injector = Guice.createInjector(new GuiceModule(properties));
        injector.injectMembers(testInstance);
    }
}
