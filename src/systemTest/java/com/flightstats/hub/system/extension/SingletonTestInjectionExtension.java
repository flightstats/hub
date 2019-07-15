package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.PropertiesLoaderOverride;
import com.flightstats.hub.system.config.GuiceModule;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.flightstats.hub.system.config.PropertiesName;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Properties;

@Builder
public class SingletonTestInjectionExtension implements TestInstancePostProcessor {
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    private String hubDockerImage;

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
        properties = new PropertiesLoaderOverride(properties)
                .withHubDockerImage(fieldOrDefault(hubDockerImage, properties.getProperty(PropertiesName.HUB_DOCKER_IMAGE)))
                .getProperties();
        Injector injector = Guice.createInjector(new GuiceModule(properties));
        injector.injectMembers(testInstance);
    }

    private String fieldOrDefault(String local, String defaultValue) {
        return StringUtils.isNotBlank(local) ? local : defaultValue;
    }
}
