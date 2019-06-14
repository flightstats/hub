package com.flightstats.hub.system.extension;

import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.config.PropertiesLoader;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Properties;

@Slf4j
public class GuiceProviderExtension implements BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.GLOBAL;

    @Override
    public void beforeAll(ExtensionContext context) {
        log.info("guice injection extension");
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        log.info("$$$$$$$$$$$$$$ injector {}", store.get("injector", Injector.class));
        if (store.get("injector", Injector.class) == null) {
            log.info("injecting stuff");
            String PROPERTY_FILE_NAME = "system-test-hub.properties";
            Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);

            store.put("properties", properties);

            Injector injector = new DependencyInjector().getOrCreateInjector(context, properties);
            log.info("injector {}", injector);
            store.put("injector", injector);
        }
    }

//    @Override
//    @SneakyThrows
//    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
//        log.info("guice injection extension");
//        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
//        if (store.get("injector") != null) {
//            log.info("injecting stuff");
//            String PROPERTY_FILE_NAME = "system-test-hub.properties";
//            Properties properties = new PropertiesLoader().loadProperties(PROPERTY_FILE_NAME);
//
//            store.put("properties", properties);
//            store.put("injector", new DependencyInjector().getOrCreateInjector(context, properties));
//        }
//    }

}
