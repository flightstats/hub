package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.GuiceModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

public class HubLifecycleSuiteExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final HubLifecycle hubLifecycle;
    private final Injector injector;
    private static final Namespace namespace = ExtensionContext.Namespace.create("SYSTEM_TEST");

    public HubLifecycleSuiteExtension() {
        injector = Guice.createInjector(new GuiceModule());
        injector.injectMembers(this);
        this.hubLifecycle = injector.getInstance(HubLifecycle.class);
    }

    @Override
    @SneakyThrows
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(Injector.class);
    }

    @Override
    @SneakyThrows
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return injector;
    }

    @Override
    @SneakyThrows
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(namespace).getOrComputeIfAbsent("setupFactory", k -> setupFactory(), HubLifecycleSetup.class);
        hubLifecycle.setup();
    }

    @Override
    @SneakyThrows
    public void afterAll(ExtensionContext context) {
        context.getRoot().getStore(namespace).getOrComputeIfAbsent("teardownFactory", k -> teardownFactory(), HubLifecycleTeardown.class);
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

    private HubLifecycleTeardown teardownFactory() {
        return new HubLifecycleTeardown(hubLifecycle);
    }

    private HubLifecycleSetup setupFactory() {
        return new HubLifecycleSetup(hubLifecycle);
    }
}
