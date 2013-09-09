package com.flightstats.datahubproxy.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HealthCheckTest {

    @Test
    public void testGet() throws Exception {
        HealthCheck testClass = new HealthCheck();
        String result = testClass.check();
        assertEquals("OK", result);
    }
}
