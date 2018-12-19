package com.flightstats.hub.metrics;

import org.junit.Test;

import java.util.Iterator;
import java.util.SortedSet;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;


public class JVMMetricsTest {
    @Test
    public void instantiationTest() {
//        JVMMetrics metricsRegister = new JVMMetrics();
//        metricsRegister.startMetricRegistry();
//        SortedSet<String> metricNames = metricsRegister.getRegisteredMetricNames();
//        Iterator iterator = metricNames.iterator();
//        while (iterator.hasNext()) {
//            String nextString = (String) iterator.next();
//            assertThat(nextString, anyOf(containsString("gc"), containsString("memory"), containsString("thread")));
//            assertThat(nextString, containsString("jvm_"));
//        }
    }


}
