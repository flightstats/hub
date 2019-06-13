package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.DependencyInjector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

@Slf4j
public class HubLifecycleSuiteExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Namespace NAMESPACE = ExtensionContext.Namespace.create("SYSTEM_TEST");

    @Override
    @SneakyThrows
    public synchronized void beforeAll(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        store.getOrComputeIfAbsent("setupFactory", k -> setupFactory(context), HubLifecycleSetup.class);
    }

    @Override
    @SneakyThrows
    public void afterAll(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        store.getOrComputeIfAbsent("teardownFactory", k -> teardownFactory(context), HubLifecycleTeardown.class);
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

    private HubLifecycle getHubLifecycle(ExtensionContext context) {
        return new DependencyInjector().getInjector(context).getInstance(HubLifecycle.class);
    }
}
