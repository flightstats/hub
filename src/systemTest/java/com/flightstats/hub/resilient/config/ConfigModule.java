package com.flightstats.hub.resilient.config;

import com.flightstats.hub.resilient.helm.ReleaseDelete;
import com.flightstats.hub.resilient.helm.ReleaseInstall;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;

import static com.flightstats.hub.PropertyLoader.load;

@Slf4j
public class ConfigModule extends AbstractModule {

    private static final String PROPERTY_FILE_NAME = "resilient-hub.properties";

    @Override
    protected void configure() {
        Names.bindProperties(binder(), load(PROPERTY_FILE_NAME));
        bind(ReleaseInstall.class);
        bind(ReleaseDelete.class);
    }
}
