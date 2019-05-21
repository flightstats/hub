package com.flightstats.hub.config;

import com.flightstats.hub.config.binding.ClusterHubBindings;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.config.binding.PropertiesBinding;
import com.flightstats.hub.config.binding.SingleHubBindings;
import com.flightstats.hub.config.binding.WebSocketBinding;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class DependencyInjection {

    public Injector init(String storageBackend) {
        Injector injector = Guice.createInjector(buildGuiceModules(storageBackend));
        injector.createChildInjector(new WebSocketBinding());
        return injector;
    }

    private List<AbstractModule> buildGuiceModules(String storageBackend) {
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new PropertiesBinding());
        modules.add(new HubBindings());
        modules.add(getGuiceModuleForHubType(storageBackend));
        return modules;
    }

    private AbstractModule getGuiceModuleForHubType(String storageBackend) {
        switch (storageBackend) {
            case "aws":
                return new ClusterHubBindings();
            case "nas":
            case "test":
                return new SingleHubBindings();
            default:
                throw new RuntimeException("unsupported hub.type " + storageBackend);
        }
    }
}