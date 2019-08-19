package com.flightstats.hub.config;

import com.amazonaws.services.dynamodbv2.xspec.S;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.aws.DynamoChannelExistenceCheck;
import com.flightstats.hub.dao.aws.DynamoWebhookExistenceCheck;
import com.flightstats.hub.dao.aws.S3WriteQueueLifecycle;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.spoke.SpokeTtlEnforcerLifecycle;
import com.google.common.util.concurrent.Service;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flightstats.hub.constant.NamedBinding.READ;
import static com.flightstats.hub.constant.NamedBinding.WRITE;

@Slf4j
public class ClusterServicesRegistration implements ServiceRegistration {

    private final StatsDReporterLifecycle statsDReporterLifecycle;
    private final DynamoChannelExistenceCheck dynamoChannelExistenceCheck;
    private final DynamoWebhookExistenceCheck dynamoWebhookExistenceCheck;
    private final SpokeTtlEnforcer spokeTtlEnforcerRead;
    private final SpokeTtlEnforcer spokeTtlEnforcerWrite;
    private final S3WriteQueueLifecycle s3WriteQueueLifecycle;
    private final CustomMetricsLifecycle customMetricsLifecycle;
    private final AppProperties appProperties;
    private final SpokeProperties spokeProperties;
    private final PeriodicMetricEmitterLifecycle periodicMetricEmitterLifecycle;

    @Inject
    public ClusterServicesRegistration(
                                       StatsDReporterLifecycle statsDReporterLifecycle,
                                       DynamoChannelExistenceCheck dynamoChannelExistenceCheck,
                                       DynamoWebhookExistenceCheck dynamoWebhookExistenceCheck,
                                       @Named(READ) SpokeTtlEnforcer spokeTtlEnforcerRead,
                                       @Named(WRITE) SpokeTtlEnforcer spokeTtlEnforcerWrite,
                                       S3WriteQueueLifecycle s3WriteQueueLifecycle,
                                       CustomMetricsLifecycle customMetricsLifecycle,
                                       AppProperties appProperties,
                                       SpokeProperties spokeProperties,
                                       PeriodicMetricEmitterLifecycle periodicMetricEmitterLifecycle) {
        this.statsDReporterLifecycle = statsDReporterLifecycle;
        this.dynamoChannelExistenceCheck = dynamoChannelExistenceCheck;
        this.dynamoWebhookExistenceCheck = dynamoWebhookExistenceCheck;
        this.spokeTtlEnforcerRead = spokeTtlEnforcerRead;
        this.spokeTtlEnforcerWrite = spokeTtlEnforcerWrite;
        this.s3WriteQueueLifecycle = s3WriteQueueLifecycle;
        this.customMetricsLifecycle = customMetricsLifecycle;
        this.appProperties = appProperties;
        this.spokeProperties = spokeProperties;
        this.periodicMetricEmitterLifecycle = periodicMetricEmitterLifecycle;
    }

    @Override
    public void register() {
        registerServices(getBeforeHealthCheckServices(), HubServices.TYPE.BEFORE_HEALTH_CHECK);
        registerServices(getAfterHealthCheckServices(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void registerServices(List<Service> services, HubServices.TYPE type) {
        services.forEach(service -> HubServices.register(service, type));
    }

    private List<Service> getBeforeHealthCheckServices() {
        List<Service> services = new ArrayList<>();
        services.add(statsDReporterLifecycle);

        if (!appProperties.isReadOnly()) {
            services.add(s3WriteQueueLifecycle);
        }

        if (spokeProperties.isTtlEnforced()) {
            services.add(new SpokeTtlEnforcerLifecycle(spokeTtlEnforcerRead));
            services.add(new SpokeTtlEnforcerLifecycle(spokeTtlEnforcerWrite));
        }

        services.addAll(Arrays.asList(dynamoChannelExistenceCheck, dynamoWebhookExistenceCheck));

        return services;
    }

    private List<Service> getAfterHealthCheckServices() {
        return Arrays.asList(customMetricsLifecycle, periodicMetricEmitterLifecycle);

    }
}
