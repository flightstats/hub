package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.GrafanaMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.IntegrationUdpServer;
import com.flightstats.hub.webhook.Webhook;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatsdReporterIntegrationTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final CountDownLatch startupCountDownLatch = new CountDownLatch(2);
    private final String[] tags = { "tag1", "tag2" };

    private IntegrationUdpServer udpServer;
    private IntegrationUdpServer udpServerDD;
    @Mock
    private TickMetricsProperties tickMetricsProperties;
    @Mock
    private MetricsProperties metricsProperties;
    @Mock
    private Dao<ChannelConfig> channelConfigDao;
    @Mock
    private Dao<Webhook> webhookDao;
    @Mock
    private GrafanaMetricsProperties grafanaMetricsProperties;

    @BeforeEach
    void setup(){
        when(grafanaMetricsProperties.getStatsdPort()).thenReturn(8122);
        when(tickMetricsProperties.getStatsdPort()).thenReturn(8123);
        udpServer = provideNewServer(tickMetricsProperties.getStatsdPort());
        udpServerDD = provideNewServer(grafanaMetricsProperties.getStatsdPort());
    }

    @SneakyThrows
    @Test
    void StatsDHandlersCount_metricShape() {

        CompletableFuture.allOf(
                getMetricsWriterFuture(),
                udpServer.getServerFuture(startupCountDownLatch, executorService),
                udpServerDD.getServerFuture(startupCountDownLatch, executorService)
        ).get(15000, TimeUnit.MILLISECONDS);


        Map<String, String> resultsStatsd = udpServer.getResult();
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsStatsd.get("hub.countTest"));

        Map<String, String> resultsDogStatsd = udpServerDD.getResult();
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsDogStatsd.get("hub.countTest"));
    }

    private IntegrationUdpServer provideNewServer(int port) {
        return IntegrationUdpServer.builder()
                .port(port)
                .build();
    }

    private CompletableFuture<String> getMetricsWriterFuture() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                startupCountDownLatch.await(10000, TimeUnit.MILLISECONDS);
                writeMetrics();
                stopServers();
                return "done";
            } catch (InterruptedException e) {
                fail("timed out waiting for startup latch " + e.getMessage());
                return e.getMessage();
            }
        }, executorService);
    }

    private void stopServers() {
        udpServer.stop();
        udpServerDD.stop();
    }

    private StatsdReporter provideStatsDHandlers() {
        StatsDFilter statsDFilter = new StatsDFilter(tickMetricsProperties, channelConfigDao, webhookDao, grafanaMetricsProperties);
        statsDFilter.setOperatingClients();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter,metricsProperties, grafanaMetricsProperties);
        return provider.get();
    }

    private void writeMetrics() {
        StatsdReporter handlers = provideStatsDHandlers();
        handlers.count("countTest", 1, tags);
    }
}
