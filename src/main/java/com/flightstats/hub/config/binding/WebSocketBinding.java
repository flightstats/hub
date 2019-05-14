package com.flightstats.hub.config.binding;

import com.google.inject.AbstractModule;

public class WebSocketBinding extends AbstractModule {

    @Override
    protected void configure() {
        requestStaticInjection(WebSocketConfigurator.class);
    }

}