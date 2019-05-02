package com.flightstats.hub.config.binding;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.AwsProperties;
import com.flightstats.hub.config.DatadogMetricsProperties;
import com.flightstats.hub.config.DynamoProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.S3Properties;
import com.flightstats.hub.config.TickMetricsProperties;
import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.config.ZookeeperProperties;
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
        bind(DatadogMetricsProperties.class).asEagerSingleton();
        bind(DynamoProperties.class).asEagerSingleton();
        bind(S3Properties.class).asEagerSingleton();
        bind(TickMetricsProperties.class).asEagerSingleton();
        bind(WebhookProperties.class).asEagerSingleton();
        bind(ZookeeperProperties.class).asEagerSingleton();
    }
}
