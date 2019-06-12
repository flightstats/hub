package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.GuiceModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

public class HubLifecycleSuiteExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Namespace NAMESPACE = ExtensionContext.Namespace.create("SYSTEM_TEST");

    private Optional<Injector> getOrCreateInjector(ExtensionContext context) {
        if (!context.getElement().isPresent()) {
            return Optional.empty();
        }

        AnnotatedElement element = context.getElement().get();
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        Injector injector = store.get(element, Injector.class);
        if (injector == null) {
            injector = Guice.createInjector(new GuiceModule());
            store.put(element, injector);
        }

        return Optional.of(injector);
    }

    private HubLifecycle getHubLifecycle(ExtensionContext context) {
        return getOrCreateInjector(context)
                .map(injector -> injector.getInstance(HubLifecycle.class))
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    @SneakyThrows
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent("setupFactory", k -> setupFactory(context), HubLifecycleSetup.class);
    }

    @Override
    @SneakyThrows
    public void afterAll(ExtensionContext context) {
        context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent("teardownFactory", k -> teardownFactory(context), HubLifecycleTeardown.class);
    }

    static class HubLifecycleSetup implements CloseableResource {
        public HubLifecycleSetup(HubLifecycle hubLifecycle) {
            hubLifecycle.setup();
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    static class HubLifecycleTeardown implements CloseableResource {
        public HubLifecycleTeardown(HubLifecycle hubLifecycle) {
            hubLifecycle.cleanup();
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    private HubLifecycleTeardown teardownFactory(ExtensionContext context) {
        return new HubLifecycleTeardown(getHubLifecycle(context));
    }

    private HubLifecycleSetup setupFactory(ExtensionContext context) {
        return new HubLifecycleSetup(getHubLifecycle(context));
    }
}
