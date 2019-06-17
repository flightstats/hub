package com.flightstats.hub.system.extension;

import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

@Slf4j
public class DependencyInjectionExtension implements BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.GLOBAL;
    @Override
    public void beforeAll(ExtensionContext context) {
        Optional.ofNullable(getInjector(context)).ifPresent(injector -> {
            injector.injectMembers(context.getTestInstance().orElseThrow(() -> new RuntimeException("test instance not available for DI" )));
        });
    }

    private Injector getInjector(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE).get("injector", Injector.class);
    }
}
