package com.flightstats.hub.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TracesTest {

    @Test
    void testLimit() {
        Traces traces = new Traces("start");
        for (int i = 0; i < 1000; i++) {
            traces.add("" + i);
        }
        List<String> output = new ArrayList<>();
        traces.outputTraces(output::add);
        assertEquals(102, output.size());
    }

}