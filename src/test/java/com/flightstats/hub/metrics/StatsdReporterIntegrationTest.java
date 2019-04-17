package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDao;
import com.flightstats.hub.dao.aws.DynamoWebhookDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.IntegrationUdpServer;
import com.flightstats.hub.webhook.Webhook;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@Slf4j
public class StatsdReporterIntegrationTest {
    private final String[] tags = { "tag1", "tag2" };
    private final MetricsConfig metricsConfig = MetricsConfig.builder()
            .hostTag("test_host")
            .statsdPort(8123)
            .dogstatsdPort(8122)
            .build();

    private final CountDownLatch startupCountDownLatch = new CountDownLatch(2);
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private final IntegrationUdpServer udpServer = provideNewServer(metricsConfig.getStatsdPort());
    private final IntegrationUdpServer udpServerDD = provideNewServer(metricsConfig.getDogstatsdPort());

    @SneakyThrows
    @Test
    public void StatsDHandlersCount_metricShape() {
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
                return "done";
            } catch (InterruptedException e) {
                fail("timed out waiting for startup latch " + e.getMessage());
                log.error("timed out waiting for startup latch {}", e);
                return e.getMessage();
            }
        }, executorService);
    }

    private StatsdReporter provideStatsDHandlers() {
        Dao<ChannelConfig> channelConfigDao = mock(DynamoChannelConfigDao.class);
        Dao<Webhook> webhookDao = mock(DynamoWebhookDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        statsDFilter.setOperatingClients();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter, metricsConfig);
        return provider.get();
    }


    private void writeMetrics() {
        StatsdReporter handlers = provideStatsDHandlers();
        handlers.count("countTest", 1, tags);
        handlers.increment("closeSocket", tags);
    }
}
