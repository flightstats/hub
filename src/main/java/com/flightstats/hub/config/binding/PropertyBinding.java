package com.flightstats.hub.config.binding;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.AwsProperty;
import com.flightstats.hub.config.DatadogMetricsProperty;
import com.flightstats.hub.config.DynamoProperty;
import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.config.TickMetricsProperty;
import com.flightstats.hub.config.WebhookProperty;
import com.flightstats.hub.config.ZookeeperProperty;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyBinding extends AbstractModule {

    @Override
    protected void configure() {

        Names.bindProperties(binder(), PropertyLoader.getInstance().getProperties());

        bind(AppProperty.class).asEagerSingleton();
        bind(AwsProperty.class).asEagerSingleton();
        bind(DatadogMetricsProperty.class).asEagerSingleton();
        bind(DynamoProperty.class).asEagerSingleton();
        bind(S3Property.class).asEagerSingleton();
        bind(TickMetricsProperty.class).asEagerSingleton();
        bind(WebhookProperty.class).asEagerSingleton();
        bind(ZookeeperProperty.class).asEagerSingleton();
    }
}
