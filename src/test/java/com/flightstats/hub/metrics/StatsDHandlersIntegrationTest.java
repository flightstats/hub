package com.flightstats.hub.metrics;

import com.flightstats.hub.util.IntegrationUdpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class StatsDHandlersIntegrationTest {
    private static IntegrationUdpServer udpServer;
    private static String[] tags = { "tag1", "tag2" };
    private static Map<String, String> results;

    private static IntegrationUdpServer provideNewServer() {
        return IntegrationUdpServer.builder()
                .timeoutMillis(4000)
                .listening(true)
                .port(8124)
                .build();
    }

    private static StatsDHandlers provideStatsDHandlers() {
        DataDogWhitelistProvider whitelistProvider = new DataDogWhitelistProvider();
        DataDogWhitelist dataDogWhitelist = whitelistProvider.get();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        statsDFilter.setOperatingClients();
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .hostTag("test_host")
                .build();
        StatsDReporterProvider provider = new StatsDReporterProvider(statsDFilter, metricsConfig);
        return provider.get();
    }

    @BeforeClass
    public static void startMockStatsDServer() {
        udpServer = provideNewServer();
        StatsDHandlers handlers = provideStatsDHandlers();
        handlers.count("countTest", 1, tags);
        handlers.gauge("gaugeTest", .1, tags);
        handlers.increment("incrementTest", tags);
        handlers.incrementCounter("incrementCounterTest", tags);
        handlers.insert("insertTestChannel",101, MetricInsert.single, 3, 1024);
        handlers.event("eventTest", "test", tags);
        handlers.time("timeTest1", 1, tags);
        handlers.time("timeName1", "timeTest2", 1, tags);
        handlers.time("timeName2", "timeTest3", 1, 1024, tags);
        results = udpServer.getResult();
        System.out.println("****************" + results);
    }

    @Test
    public void StatsDHandlersCount_metricShape() {
        assertEquals(results.get("hub.countTest"), "hub.countTest:1|c|#tag2,tag1");
    }

    @Test
    public void StatsDHandlersGauge_metricShape() {
        assertEquals(results.get("hub.gaugeTest"), "hub.gaugeTest:0.1|g|#tag2,tag1");
    }

    @Test
    public void StatsDHandlersIncrement_metricShape() {
        assertEquals(results.get("hub.incrementTest"), "hub.incrementTest:1|c|#tag2,tag1");
    }

    @Test
    public void StatsDHandlersIncrementCounter_metricShape() {
        assertEquals(results.get("hub.incrementCounterTest"), "hub.incrementCounterTest:1|c|#tag2,tag1");
    }

    @Test
    public void StatsDHandlersIncrementInsert_metricShape() {
        assertThat(results.get("hub.channel"), matchesRegex("hub\\.channel\\:\\d+\\|ms\\|#channel\\:insertTestChannel,type\\:single"));
        assertEquals(results.get("hub.channel.items"), "hub.channel.items:3|c|#channel:insertTestChannel,type:single");
        assertEquals(results.get("hub.channel.bytes"), "hub.channel.bytes:1024|c|#channel:insertTestChannel,type:single");
    }

    @Test
    public void StatsDHandlersEvent_metricShape() {
        assertEquals(results.get("_e{13,4}"), "_e{13,4}:hub.eventTest|test|h:test_host|p:normal|t:warning|#tag2,tag1");
    }

    @Test
    // signature (String name, String... tags)
    public void StatsDHandlersTime1_metricShape() {
        assertThat(results.get("hub.timeTest1"), matchesRegex("hub\\.timeTest1:\\d+\\|ms\\|#tag2,tag1"));
    }

    @Test
    // signature (String channel, String name, long start, String... tags)
    public void StatsDHandlersTime2_metricShape() {
        assertThat(results.get("hub.timeTest2"), matchesRegex("hub\\.timeTest2:\\d+\\|ms\\|#channel:timeName1,tag2,tag1"));
    }

    @Test
    // signature (String channel, String name, long start, long bytes, String... tags)
    public void StatsDHandlersTime3_metricShape() {
        assertThat(results.get("hub.timeTest3"), matchesRegex("hub\\.timeTest3:\\d+\\|ms\\|#channel:timeName2,tag2,tag1"));
        assertEquals(results.get("hub.timeTest3.bytes"), "hub.timeTest3.bytes:1024|c|#channel:timeName2,tag2,tag1");
    }

    @AfterClass
    public static void shutDownServer() {
        udpServer.closeServer();
    }
}
