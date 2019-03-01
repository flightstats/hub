package com.flightstats.hub.callback;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("callback")
public class JerseyAppConfig extends ResourceConfig {

    public JerseyAppConfig() {

        packages("com.flightstats.hub.callback");
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CacheObject.class).to(CacheObject.class).in(Singleton.class);
            }
        });
    }
}