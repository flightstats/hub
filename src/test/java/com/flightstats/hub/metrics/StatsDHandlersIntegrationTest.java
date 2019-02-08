package com.flightstats.hub.metrics;

import com.flightstats.hub.util.IntegrationUdpServer;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StatsDHandlersIntegrationTest {
    private IntegrationUdpServer udpServer;

    @BeforeClass
    public void startMockStatsDServer() {
        udpServer = IntegrationUdpServer.builder()
                .timeoutMillis(1000)
                .port(8124)
                .build();
    }

    @Test
    public void StatsDHandler_reportsMetric() {
        DataDogWhitelistProvider whitelistProvider = new DataDogWhitelistProvider();
        DataDogWhitelist dataDogWhitelist = whitelistProvider.get();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        statsDFilter.setOperatingClients();
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .hostTag("test_host")
                .build();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter, metricsConfig);
        StatsDHandlers handlers = provider.get();
        handlers.count("testMetric", 1, "tag1", "tag2");
        String result = udpServer.getAsyncResult();
        System.out.println(result);
        assertEquals(result, "hub.testMetric:1|c|#tag2,tag1");
    }
}
