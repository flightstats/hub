package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.GuiceModule;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Properties;

@Slf4j
public class GuiceProviderExtension implements BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.GLOBAL;
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        store.getOrComputeIfAbsent("injector", (thing) -> createInjector(), Injector.class);
    }

    private Injector createInjector() {
        log.debug("loading properties and instantiating DI");
        Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
        return Guice.createInjector(new GuiceModule(properties));
    }
}
