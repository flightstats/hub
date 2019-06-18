package com.flightstats.hub.system.extension;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.google.inject.Injector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

@Slf4j
public class HubLifecycleSuiteExtension implements BeforeAllCallback {
    private static final Namespace NAMESPACE = Namespace.GLOBAL;
    private static final AtomicReference<HubLifecycle> hubLifecycle = new AtomicReference<>();

    @Override
    @SneakyThrows
    public void beforeAll(ExtensionContext context) {
        log.debug("before test execution");
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        setHubLifecycle(context);

        store.getOrComputeIfAbsent(HubLifecycleSetup.class);
    }

    static class HubLifecycleSetup implements CloseableResource, AutoCloseable {
        private static final AtomicInteger creations = new AtomicInteger();

        @SuppressWarnings("unused") // used via reflection
        HubLifecycleSetup() {
            log.debug("setting up hub lifecycle");
            hubLifecycle.get().setup();
            assertEquals(1, creations.incrementAndGet(), "Singleton pattern failure!");
        }

        @Override
        public void close() {
            log.debug("tearing down hub lifecycle");
            Optional.ofNullable(hubLifecycle.get())
                    .ifPresent(HubLifecycle::cleanup);
        }
    }

    private void setHubLifecycle(ExtensionContext context) {
        if (hubLifecycle.get() == null) {
            Injector injector = context.getRoot()
                    .getStore(NAMESPACE)
                    .get("injector", Injector.class);
            hubLifecycle.set(injector.getInstance(HubLifecycle.class));
        }
    }
}
