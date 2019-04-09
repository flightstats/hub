package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.IntegrationUdpServer;
import com.flightstats.hub.webhook.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Time;
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

@Slf4j
public class StatsdReporterIntegrationTest {
    private String[] tags = { "tag1", "tag2" };
    private Map<String, String> resultsStatsd;
    private Map<String, String> resultsDogStatsd;
    private MetricsConfig metricsConfig = MetricsConfig.builder()
            .hostTag("test_host")
            .statsdPort(8123)
            .dogstatsdPort(8122)
            .build();

    private IntegrationUdpServer provideNewServer(int port, CountDownLatch startupCountDownLatch, AtomicBoolean canListen, ExecutorService executorService) {
        return IntegrationUdpServer.builder()
                .listening(new AtomicBoolean(true))
                .port(port)
                .startupCountDownLatch(startupCountDownLatch)
                .canListen(canListen)
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

    public void startMockStatsDServer() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch startupCountDownLatch = new CountDownLatch(2);
        AtomicBoolean canListen = new AtomicBoolean();
        StatsdReporter handlers = provideStatsDHandlers();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        IntegrationUdpServer udpServer = provideNewServer(metricsConfig.getStatsdPort(), startupCountDownLatch, canListen, executorService);
        IntegrationUdpServer udpServerDD = provideNewServer(metricsConfig.getDogstatsdPort(), startupCountDownLatch, canListen, executorService);
        CompletableFuture<String> sender = CompletableFuture.supplyAsync(() -> {
            try {
                startupCountDownLatch.await(15000, TimeUnit.MILLISECONDS);
                log.info("yay");
                log.info(Long.valueOf(startupCountDownLatch.getCount()).toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e.getMessage());
                fail();
            }
            log.info("gonna count now");
            handlers.count("countTest", 1, tags);
            handlers.increment("closeSocket", tags);
            log.info("done with handlers");
            canListen.set(true);
            return "done";
        }, executorService);
        log.info("about to get futures");
        CompletableFuture.allOf(sender, udpServer.getServerFuture(), udpServerDD.getServerFuture())
                .get(15000, TimeUnit.MILLISECONDS);
        log.info(String.valueOf(startupCountDownLatch.getCount()));
        Thread.sleep(500);
        resultsStatsd = udpServer.getResult();
        resultsDogStatsd = udpServerDD.getResult();
    }

    @Test
    public void StatsDHandlersCount_metricShape() throws InterruptedException, ExecutionException, TimeoutException {
        startMockStatsDServer();
        log.info("asserting");
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsStatsd.get("hub.countTest"));
        assertEquals("hub.countTest:1|c|#tag2,tag1", resultsDogStatsd.get("hub.countTest"));
    }

}
