package com.flightstats.hub.config.binding;

import com.flightstats.hub.config.properties.*;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertiesBinding extends AbstractModule {

    @Override
    protected void configure() {

        Names.bindProperties(binder(), PropertiesLoader.getInstance().getProperties());

        bind(AppProperties.class).asEagerSingleton();
        bind(AwsProperties.class).asEagerSingleton();
        bind(ContentProperties.class).asEagerSingleton();
        bind(GrafanaMetricsProperties.class).asEagerSingleton();
        bind(DynamoProperties.class).asEagerSingleton();
        bind(S3Properties.class).asEagerSingleton();
        bind(SpokeProperties.class).asEagerSingleton();
        bind(SystemProperties.class).asEagerSingleton();
        bind(TickMetricsProperties.class).asEagerSingleton();
        bind(WebhookProperties.class).asEagerSingleton();
        bind(ZooKeeperProperties.class).asEagerSingleton();
    }
}
