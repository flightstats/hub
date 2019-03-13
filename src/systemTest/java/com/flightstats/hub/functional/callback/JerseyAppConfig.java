package com.flightstats.hub.functional.callback;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("callback")
public class JerseyAppConfig extends ResourceConfig {

    public JerseyAppConfig() {

        packages("com.flightstats.hub.functional.callback");
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CallbackCache.class).to(CallbackCache.class).in(Singleton.class);
            }
        });
    }
}