package com.flightstats.hub.config;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.app.StorageBackend;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDaoLifecycle;
import com.flightstats.hub.dao.aws.DynamoWebhookDaoLifecycle;
import com.flightstats.hub.dao.aws.S3WriteQueueLifecycle;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.spoke.SpokeTtlEnforcerLifecycle;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.flightstats.hub.spoke.SpokeStore.READ;
import static com.flightstats.hub.spoke.SpokeStore.WRITE;

public class ServiceRegistration {

    public void register(StorageBackend storageBackend, Injector injector) {
        registerServices(getBeforeHealthCheckServices(storageBackend, injector), HubServices.TYPE.BEFORE_HEALTH_CHECK);
        registerServices(getAfterHealthCheckServices(storageBackend, injector), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void registerServices(List<Service> services, HubServices.TYPE type) {
        services.forEach(service -> HubServices.register(service, type));
    }

    private List<Service> getBeforeHealthCheckServices(StorageBackend storageBackend, Injector injector) {
        List<Service> services = createInstanceList(
                injector,
                InfluxdbReporterLifecycle.class,
                StatsDReporterLifecycle.class);

        AppProperties appProperties = injector.getInstance(AppProperties.class);
        if (storageBackend == StorageBackend.aws) {
            if (!appProperties.isReadOnly()) {
                services.add(injector.getInstance(S3WriteQueueLifecycle.class));
            }
            services.addAll(createInstanceList(injector,
                    DynamoChannelConfigDaoLifecycle.class,
                    DynamoWebhookDaoLifecycle.class));
        }

        SpokeProperties spokeProperties = injector.getInstance(SpokeProperties.class);
        if (spokeProperties.isTtlEnforced()) {
            SpokeTtlEnforcer spokeTtlEnforcerRead =
                    injector.getInstance(Key.get(SpokeTtlEnforcer.class, Names.named(READ.name())));
            services.add(new SpokeTtlEnforcerLifecycle(spokeTtlEnforcerRead));

            SpokeTtlEnforcer spokeTtlEnforcerWrite =
                    injector.getInstance(Key.get(SpokeTtlEnforcer.class, Names.named(WRITE.name())));
            services.add(new SpokeTtlEnforcerLifecycle(spokeTtlEnforcerWrite));
        }

        return services;
    }

    private List<Service> getAfterHealthCheckServices(StorageBackend storageBackend, Injector injector) {
        if (storageBackend != StorageBackend.aws) {
            return Collections.singletonList(injector.getInstance(CustomMetricsLifecycle.class));
        }
        return createInstanceList(injector,
                CustomMetricsLifecycle.class,
                PeriodicMetricEmitterLifecycle.class
        );
    }

    private List<Service> createInstanceList(Injector injector, Class<? extends Service>... classes) {
        return Stream.of(classes).map(injector::getInstance).collect(Collectors.toList());
    }

}