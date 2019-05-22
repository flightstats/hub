package com.flightstats.hub.config.binding;

import com.google.inject.Injector;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpointConfig;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Inject
    private static Injector injector;

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return injector.getInstance(endpointClass);
    }
}
