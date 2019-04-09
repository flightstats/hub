package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.IntegrationUdpServer;
import com.flightstats.hub.webhook.Webhook;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class StatsdReporterIntegrationTest {
    private static String[] tags = { "tag1", "tag2" };
    private static Map<String, String> resultsStatsd;
    private static Map<String, String> resultsDogStatsd;
    private static MetricsConfig metricsConfig = MetricsConfig.builder()
            .hostTag("test_host")
            .statsdPort(8123)
            .dogstatsdPort(8122)
            .build();

    private static IntegrationUdpServer provideNewServer(int port) {
        return IntegrationUdpServer.builder()
                .timeoutMillis(5000)
                .listening(true)
                .port(port)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static StatsdReporter provideStatsDHandlers() {
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        statsDFilter.setOperatingClients();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter, metricsConfig);
        return provider.get();
    }

    private static void runAfterSocketInit(Runnable runnable) {
        Executors.newScheduledThreadPool(1).schedule(runnable, 100, TimeUnit.MILLISECONDS);
    }


    @BeforeClass
    public static void startMockStatsDServer() {
        StatsdReporter handlers = provideStatsDHandlers();
        IntegrationUdpServer udpServer = provideNewServer(metricsConfig.getStatsdPort());
        IntegrationUdpServer udpServerDD = provideNewServer(metricsConfig.getDogstatsdPort());
        runAfterSocketInit(() -> {
            handlers.count("countTest", 1, tags);
            handlers.increment("closeSocket", tags);
        });
        resultsStatsd = udpServer.getResult();
        resultsDogStatsd = udpServerDD.getResult();
    }

    @Test
    public void StatsDHandlersCount_metricShape() {
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsStatsd.get("hub.countTest"));
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsDogStatsd.get("hub.countTest"));
    }

}
