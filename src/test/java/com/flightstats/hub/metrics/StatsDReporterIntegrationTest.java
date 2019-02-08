package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.IntegrationUdpServer;
import org.junit.After;
import org.junit.Before;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StatsDReporterIntegrationTest {
    private IntegrationUdpServer udpServer;

    @Before
    public void startMockDataDogServer() throws IOException {
        udpServer = new IntegrationUdpServer(8124);
        udpServer.startServer();
    }

    @Test
    public void testServer() throws InterruptedException {
        HubProperties.setProperty("metrics.filter.include.patterns", "a,b,c");
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
        Thread.sleep(2000);
        System.out.println("********");
        assertEquals(udpServer.getResults(), "hub.testMetric:1|c|#tag2,tag1");
    }

    @After
    public void stopServer() {
        udpServer.stop();
    }
}
