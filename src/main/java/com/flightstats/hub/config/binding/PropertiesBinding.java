package com.flightstats.hub.config.binding;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.AwsProperties;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.DynamoProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.config.properties.SystemProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.config.properties.ZooKeeperProperties;
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
        bind(DatadogMetricsProperties.class).asEagerSingleton();
        bind(DynamoProperties.class).asEagerSingleton();
        bind(S3Properties.class).asEagerSingleton();
        bind(SpokeProperties.class).asEagerSingleton();
        bind(SystemProperties.class).asEagerSingleton();
        bind(TickMetricsProperties.class).asEagerSingleton();
        bind(WebhookProperties.class).asEagerSingleton();
        bind(ZooKeeperProperties.class).asEagerSingleton();
    }
}
