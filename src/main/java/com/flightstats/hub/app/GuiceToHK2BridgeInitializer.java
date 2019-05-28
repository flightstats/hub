package com.flightstats.hub.app;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class GuiceToHK2BridgeInitializer implements Feature {
    private final ServiceLocator serviceLocator;

    @Inject
    public GuiceToHK2BridgeInitializer(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    private void initGuiceIntoHK2Bridge() {
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(HubMain.getInjector());
    }

    @Override
    public boolean configure(FeatureContext context) {
        initGuiceIntoHK2Bridge();
        return true;
    }
}
