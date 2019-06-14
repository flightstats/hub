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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

@Slf4j
public class HubLifecycleSuiteExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Namespace NAMESPACE = Namespace.GLOBAL;
    private static final AtomicReference<HubLifecycle> hubLifecycle = new AtomicReference<>();

    @Override
    @SneakyThrows
    public void beforeAll(ExtensionContext context) {
        log.info("before test execution");
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        setHubLifecycle(context);

        store.getOrComputeIfAbsent(HubLifecycleSetup.class);
    }

    @Override
    @SneakyThrows
    public void afterAll(ExtensionContext context) {
        log.info("after all extension");
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        setHubLifecycle(context);
        store.getOrComputeIfAbsent(HubLifecycleTeardown.class);
    }

    static class HubLifecycleSetup implements CloseableResource {
        HubLifecycleSetup() {
            hubLifecycle.get().setup();
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    static class HubLifecycleTeardown implements CloseableResource {
        HubLifecycleTeardown() {
            Optional.ofNullable(hubLifecycle.get()).ifPresent(HubLifecycle::cleanup);
        }

        @Override
        public void close() {
            // do nothing
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
