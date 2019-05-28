package com.flightstats.hub.config;

import com.flightstats.hub.app.HubServices;
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
    private final CustomMetricsLifecycle customMetricsLifecycle;

    @Inject
    public SingleServicesRegistration(InfluxdbReporterLifecycle influxdbReporterLifecycle,
                                      StatsDReporterLifecycle statsDReporterLifecycle,
                                      CustomMetricsLifecycle customMetricsLifecycle) {
        this.influxdbReporterLifecycle = influxdbReporterLifecycle;
        this.statsDReporterLifecycle = statsDReporterLifecycle;
        this.customMetricsLifecycle = customMetricsLifecycle;
    }

    @Override
    public void register() {
        registerServices(getBeforeHealthCheckServices(), HubServices.TYPE.BEFORE_HEALTH_CHECK);
        registerServices(getAfterHealthCheckServices(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    void registerServices(List<Service> services, HubServices.TYPE type) {
        services.forEach(service -> HubServices.register(service, type));
    }

    List<Service> getBeforeHealthCheckServices() {
        return Arrays.asList(influxdbReporterLifecycle, statsDReporterLifecycle);
    }

    List<Service> getAfterHealthCheckServices() {
        return Arrays.asList(customMetricsLifecycle);
    }
}