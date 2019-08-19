package com.flightstats.hub.config;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.metrics.CustomMetricsLifecycle;
import com.flightstats.hub.metrics.StatsDReporterLifecycle;
import com.google.common.util.concurrent.Service;
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

import java.util.List;
import java.util.Map;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;
import static com.flightstats.hub.app.HubServices.TYPE.BEFORE_HEALTH_CHECK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingleServicesRegistrationTest {

    @Mock
    private StatsDReporterLifecycle statsDReporterLifecycle;
    @Mock
    private CustomMetricsLifecycle customMetricsLifecycle;
    private SingleServicesRegistration singleServicesRegistration;

    @BeforeEach
    void setup() {
        singleServicesRegistration = new SingleServicesRegistration(
                statsDReporterLifecycle,
                customMetricsLifecycle);
        HubServices.clear();
    }

    @Test
    @Order(1)
    void testRegister() {
        singleServicesRegistration.register();

        Map<HubServices.TYPE, List<Service>> serviceMap = HubServices.getServices();

        assertEquals(serviceMap.get(BEFORE_HEALTH_CHECK).size(), 1);
        assertTrue(serviceMap.get(BEFORE_HEALTH_CHECK)
                .contains(statsDReporterLifecycle));

        assertEquals(serviceMap.get(AFTER_HEALTHY_START).size(), 1);
        assertTrue(serviceMap.get(AFTER_HEALTHY_START).contains(customMetricsLifecycle));
    }

    @Test
    @Order(2)
    void testBeforeHealthCheckRegisterServices() {
        List<Service> actualServices = singleServicesRegistration.getBeforeHealthCheckServices();
        singleServicesRegistration.registerServices(actualServices, BEFORE_HEALTH_CHECK);
        List<Service> expectedServices = HubServices.getServices().get(BEFORE_HEALTH_CHECK);
        assertEquals(expectedServices, actualServices);
    }

    @Test
    @Order(3)
    void testAfterHealthCheckRegisterServices() {
        List<Service> actualServices = singleServicesRegistration.getAfterHealthCheckServices();
        singleServicesRegistration.registerServices(actualServices, AFTER_HEALTHY_START);
        List<Service> expectedServices = HubServices.getServices().get(AFTER_HEALTHY_START);
        assertEquals(expectedServices, actualServices);
    }

    @Test
    @Order(4)
    void testGetBeforeHealthCheckServices() {
        List<Service> services = singleServicesRegistration.getBeforeHealthCheckServices();
        assertEquals(services.size(), 1);
        assertTrue(services.contains(statsDReporterLifecycle));
    }

    @Test
    @Order(5)
    void testGetAfterHealthCheckServices() {
        List<Service> services = singleServicesRegistration.getAfterHealthCheckServices();
        assertEquals(services.size(), 1);
        assertTrue(services.contains(customMetricsLifecycle));
    }

    @AfterEach
    void cleanup() {
        HubServices.clear();
    }
}