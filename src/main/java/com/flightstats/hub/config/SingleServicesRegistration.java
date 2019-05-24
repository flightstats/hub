package com.flightstats.hub.config;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.app.StorageBackend;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.google.common.util.concurrent.Service;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class SingleServicesRegistration implements ServiceRegistration {

    private final InfluxdbReporterLifecycle influxdbReporterLifecycle;
    private final StatsDReporterLifecycle statsDReporterLifecycle;
    private final AppProperties appProperties;

    @Inject
    private CustomMetricsLifecycle customMetricsLifecycle;


    @Inject
    public SingleServicesRegistration(InfluxdbReporterLifecycle influxdbReporterLifecycle,
                               StatsDReporterLifecycle statsDReporterLifecycle,
                               CustomMetricsLifecycle customMetricsLifecycle,
                               AppProperties appProperties) {
        this.influxdbReporterLifecycle = influxdbReporterLifecycle;
        this.statsDReporterLifecycle = statsDReporterLifecycle;
        this.customMetricsLifecycle = customMetricsLifecycle;

        this.appProperties = appProperties;
    }

    @Override
    public void register() {
        StorageBackend storageBackend = StorageBackend.valueOf(appProperties.getHubType());
        log.info("Hub server starting with hub.type {}", storageBackend.toString());

        registerServices(getBeforeHealthCheckServices(storageBackend), HubServices.TYPE.BEFORE_HEALTH_CHECK);
        registerServices(getAfterHealthCheckServices(storageBackend), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void registerServices(List<Service> services, HubServices.TYPE type) {
        services.forEach(service -> HubServices.register(service, type));
    }

    private List<Service> getBeforeHealthCheckServices(StorageBackend storageBackend) {
        return Arrays.asList(influxdbReporterLifecycle, statsDReporterLifecycle);
    }

    private List<Service> getAfterHealthCheckServices(StorageBackend storageBackend) {
        return Arrays.asList(customMetricsLifecycle);
    }
}