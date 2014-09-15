package com.flightstats.hub.metrics;

import com.codahale.metrics.Counter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HostedGraphiteReporterTest {

    @Test
    public void testCounter() throws Exception {
        HostedGraphiteReporter reporter = HostedGraphiteReporter.forRegistry(null).build(null);
        Counter counter = new Counter();
        counter.inc();
        assertEquals("1", reporter.intervalCount("test", counter));
        counter.inc();
        assertEquals("1",reporter.intervalCount("test", counter));
        counter.inc();
        assertEquals("1",reporter.intervalCount("test", counter));
    }

}