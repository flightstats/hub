package com.flightstats.hub.config;

import com.google.inject.Injector;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorListener;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import java.util.Set;

public class GuiceToHK2BridgeInitializer implements ServiceLocatorListener {
    private final Injector injector;

    public GuiceToHK2BridgeInitializer(Injector injector) {
        this.injector = injector;
    }

    private void initGuiceIntoHK2Bridge(ServiceLocator serviceLocator) {
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);
    }

    @Override
    public void initialize(Set<ServiceLocator> initialLocators) {}

    @Override
    public void locatorAdded(ServiceLocator added) {
        initGuiceIntoHK2Bridge(added);
    }

    @Override
    public void locatorDestroyed(ServiceLocator destroyed) {}
}
