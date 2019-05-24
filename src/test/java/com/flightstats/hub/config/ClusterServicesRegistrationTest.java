package com.flightstats.hub.config;

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
import com.google.common.util.concurrent.Service;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;
import static com.flightstats.hub.app.HubServices.TYPE.BEFORE_HEALTH_CHECK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ClusterServicesRegistrationTest {

    @Mock
    private InfluxdbReporterLifecycle influxdbReporterLifecycle;
    @Mock
    private StatsDReporterLifecycle statsDReporterLifecycle;
    @Mock
    private DynamoChannelExistenceCheck dynamoChannelExistenceCheck;
    @Mock
    private DynamoWebhookExistenceCheck dynamoWebhookExistenceCheck;
    @Mock
    private SpokeTtlEnforcer spokeTtlEnforcerRead;
    @Mock
    private SpokeTtlEnforcer spokeTtlEnforcerWrite;
    @Mock
    private S3WriteQueueLifecycle s3WriteQueueLifecycle;
    @Mock
    private CustomMetricsLifecycle customMetricsLifecycle;
    @Mock
    private AppProperties appProperties;
    @Mock
    private SpokeProperties spokeProperties;
    @Mock
    private PeriodicMetricEmitterLifecycle periodicMetricEmitterLifecycle;
    private ClusterServicesRegistration clusterServicesRegistration;

    @BeforeEach
    void setup() {
        clusterServicesRegistration = new ClusterServicesRegistration(
                influxdbReporterLifecycle,
                statsDReporterLifecycle,
                dynamoChannelExistenceCheck,
                dynamoWebhookExistenceCheck,
                spokeTtlEnforcerRead,
                spokeTtlEnforcerWrite,
                s3WriteQueueLifecycle,
                customMetricsLifecycle,
                appProperties,
                spokeProperties,
                periodicMetricEmitterLifecycle);
        HubServices.clear();
    }

    @Test
    @Order(1)
    void testRegisterForReadWriteMode() {
        when(appProperties.isReadOnly()).thenReturn(false);
        when(spokeProperties.isTtlEnforced()).thenReturn(true);

        clusterServicesRegistration.register();

        Map<HubServices.TYPE, List<Service>> serviceMap = HubServices.getServices();

        assertEquals(serviceMap.get(BEFORE_HEALTH_CHECK).size(), 7);
        assertTrue(serviceMap.get(BEFORE_HEALTH_CHECK)
                .containsAll(Arrays.asList(influxdbReporterLifecycle,
                        statsDReporterLifecycle,
                        s3WriteQueueLifecycle,
                        dynamoChannelExistenceCheck,
                        dynamoWebhookExistenceCheck)));


        assertEquals(serviceMap.get(AFTER_HEALTHY_START).size(), 2);
        assertTrue(serviceMap.get(AFTER_HEALTHY_START)
                .containsAll(Arrays.asList(customMetricsLifecycle, periodicMetricEmitterLifecycle)));
    }


    @Test
    @Order(1)
    void testRegisterForReadOnlyMode() {
        when(appProperties.isReadOnly()).thenReturn(true);
        when(spokeProperties.isTtlEnforced()).thenReturn(true);

        clusterServicesRegistration.register();

        Map<HubServices.TYPE, List<Service>> serviceMap = HubServices.getServices();

        assertEquals(serviceMap.get(BEFORE_HEALTH_CHECK).size(), 6);
        assertTrue(serviceMap.get(BEFORE_HEALTH_CHECK)
                .containsAll(Arrays.asList(influxdbReporterLifecycle,
                        statsDReporterLifecycle,
                        dynamoChannelExistenceCheck,
                        dynamoWebhookExistenceCheck)));


        assertEquals(serviceMap.get(AFTER_HEALTHY_START).size(), 2);
        assertTrue(serviceMap.get(AFTER_HEALTHY_START)
                .containsAll(Arrays.asList(customMetricsLifecycle, periodicMetricEmitterLifecycle)));
    }

    @Test
    @Order(3)
    void testRegisterForSpokeTtlDisbaled() {
        when(appProperties.isReadOnly()).thenReturn(false);
        when(spokeProperties.isTtlEnforced()).thenReturn(false);

        clusterServicesRegistration.register();

        Map<HubServices.TYPE, List<Service>> serviceMap = HubServices.getServices();

        assertEquals(serviceMap.get(BEFORE_HEALTH_CHECK).size(), 5);
        assertTrue(serviceMap.get(BEFORE_HEALTH_CHECK)
                .containsAll(Arrays.asList(influxdbReporterLifecycle,
                        statsDReporterLifecycle,
                        s3WriteQueueLifecycle,
                        dynamoChannelExistenceCheck,
                        dynamoWebhookExistenceCheck)));

        assertEquals(serviceMap.get(AFTER_HEALTHY_START).size(), 2);
        assertTrue(serviceMap.get(AFTER_HEALTHY_START)
                .containsAll(Arrays.asList(customMetricsLifecycle, periodicMetricEmitterLifecycle)));
    }

   @AfterEach
    void cleanup() {
        HubServices.clear();
    }

}