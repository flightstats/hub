package com.flightstats.hub.system.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;
import java.util.Properties;

@Slf4j
public class DependencyInjector {
    Injector getOrCreateInjector(ExtensionContext context, Properties properties) {
        if (!context.getElement().isPresent()) {
            log.info("dunno why this is here but it happened");
            return null;
        }

        ExtensionContext.Store store = getGlobalStore(context);

        return Optional.ofNullable(store.get("injector", Injector.class))
                .orElse(Guice.createInjector(new GuiceModule(properties)));
    }

    private ExtensionContext.Store getGlobalStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
    }
}
