package com.flightstats.hub.time;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.MetricsService;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class NtpMonitorTest {

    @Test
    public void testPositive() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "     remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "+util            91.189.94.4      3 u  545 1024  377   99.523    1.032   0.631",
                "*n2              20.9.10.5        4 u  996 1024  377    1.527    1.128   0.371",
                "+hub-v2-01       20.20.20.5       5 u  622 1024  337    0.269    0.606   0.449",
                "-hub-v2-02       20.20.20.5       5 u 1026 1024  376    0.390    0.277   0.146"
        };
        assertEquals(0.606, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(1.080, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

    @Test
    public void testNegative() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "     remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "+util            91.189.94.4      3 u  545 1024  377   99.523   -1.032   0.631",
                "*n2              20.9.10.5        4 u  996 1024  377    1.527   -1.128   0.371",
                "+hub-v2-01       20.20.20.5       5 u  622 1024  337    0.269   -0.606   0.449",
                "-hub-v2-02       20.20.20.5       5 u 1026 1024  376    0.390   -0.277   0.146"
        };
        assertEquals(0.606, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(-1.080, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

    @Test
    public void testPlusMinus() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "     remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "+util            91.189.94.4      3 u  545 1024  377   99.523   -1.032   0.631",
                "*n2              20.9.10.5        4 u  996 1024  377    1.527   1.128   0.371",
                "+hub-v2-01       20.20.20.5       5 u  622 1024  337    0.269    0.606   0.449",
                "-hub-v2-02       20.20.20.5       5 u 1026 1024  376    0.390   -0.277   0.146"
        };
        assertEquals(0.883, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(0.048, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

    @Test
    public void testSingleServer() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "*n2             20.9.10.5        4 u  898 1024  377    1.538   -1.325   1.167"
        };
        assertEquals(0.0, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(-1.325, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

    @Test
    public void testPositiveSelf() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "     remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "+util            91.189.94.4      3 u  545 1024  377   99.523   -1.032   0.631",
                "*n2              20.9.10.5        4 u  996 1024  377    1.527   -1.128   0.371",
                "+hub-v2-01       20.20.20.5       5 u  622 1024  337    0.269    0.606   0.449",
                "-hub-v2-02       20.20.20.5       5 u 1026 1024  376    0.390    0.277   0.146",
                " hub-v2-03       .INIT.          16 u    -   64    0    0.000    0.000   0.000"
        };
        assertEquals(0.606, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(-1.080, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

    @Test
    public void testIps() {
        MetricsService metricsService = mock(MetricsService.class);
        HubProperties hubProperties = mock(HubProperties.class);
        NtpMonitor ntpMonitor = new NtpMonitor(metricsService, hubProperties);
        String[] output = {
                "     remote           refid      st t when poll reach   delay   offset  jitter",
                "==============================================================================",
                "-nexus9k-1.util. 10.1.11.101      3 u   39   64  377    0.332    0.171   0.490",
                "*nexus9k-2.util. 10.1.11.100      3 u   23   64  377    0.331    0.013   0.553",
                " hub-ucs.pdx.    .STEP.          16 u    -   64    0    0.000    0.000   0.000",
                "+10.10.1.54      10.1.11.4        4 u   41   64  377    0.167    0.230   0.572",
                "+10.10.1.59      10.1.11.4        4 u   56   64  376    0.211    0.237   0.463"
        };
        assertEquals(0.237, ntpMonitor.parseClusterRange(Arrays.asList(output)), 0.001);
        assertEquals(0.013, ntpMonitor.parsePrimary(Arrays.asList(output)), 0.001);
    }

}
