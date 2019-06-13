package com.flightstats.hub.system.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

@Slf4j
public class DependencyInjector {
    public Injector getInjector(ExtensionContext context) {
        return getOrCreateInjector(context)
                .orElseThrow(IllegalStateException::new);
    }

    private Optional<Injector> getOrCreateInjector(ExtensionContext context) {
        if (!context.getElement().isPresent()) {
            return Optional.empty();
        }

        ExtensionContext.Store store = getGlobalStore(context);

        Injector injector = store.get("injector", Injector.class);
        if (injector == null) {
            log.info("creating guice injector and injecting all the things");
            injector = Guice.createInjector(new GuiceModule());
            injector.injectMembers(new GuiceModule());
            store.put("injector", injector);
        }

        return Optional.of(injector);
    }

    private ExtensionContext.Store getGlobalStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
    }
}
