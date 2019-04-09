package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.IntegrationUdpServer;
import com.flightstats.hub.webhook.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class StatsdReporterIntegrationTest {
    private String[] tags = { "tag1", "tag2" };
    private MetricsConfig metricsConfig = MetricsConfig.builder()
            .hostTag("test_host")
            .statsdPort(8123)
            .dogstatsdPort(8122)
            .build();

    private final CountDownLatch startupCountDownLatch = new CountDownLatch(2);
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private final IntegrationUdpServer udpServer = provideNewServer(metricsConfig.getStatsdPort());
    private final IntegrationUdpServer udpServerDD = provideNewServer(metricsConfig.getDogstatsdPort());

    private IntegrationUdpServer provideNewServer(int port) {
        return IntegrationUdpServer.builder()
                .listening(true)
                .port(port)
                .startupCountDownLatch(startupCountDownLatch)
                .executorService(executorService)
                .build();
    }

    @SuppressWarnings("unchecked")
    private StatsdReporter provideStatsDHandlers() {
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        statsDFilter.setOperatingClients();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter, metricsConfig);
        return provider.get();
    }

    @Test
    public void StatsDHandlersCount_metricShape() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> metricsWriter = getMetricsWriterFuture();
        CompletableFuture.allOf(metricsWriter, udpServer.getServerFuture(), udpServerDD.getServerFuture())
                .get(15000, TimeUnit.MILLISECONDS);

        Map<String, String> resultsStatsd = udpServer.getResult();
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsStatsd.get("hub.countTest"));

        Map<String, String> resultsDogStatsd = udpServerDD.getResult();
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsDogStatsd.get("hub.countTest"));
    }

    private CompletableFuture<String> getMetricsWriterFuture() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                startupCountDownLatch.await(15000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            writeMetrics();
            return "done";
        }, executorService);
    }

    private void writeMetrics() {
        StatsdReporter handlers = provideStatsDHandlers();
        handlers.count("countTest", 1, tags);
        handlers.increment("closeSocket", tags);
    }
}
