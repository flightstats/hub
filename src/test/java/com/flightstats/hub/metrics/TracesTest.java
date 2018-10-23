package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TracesTest {

    @Test
    public void testLimit() {
        HubProperties hubProperties = mock(HubProperties.class);
        when(hubProperties.getProperty("traces.limit", 50)).thenReturn(50);
        Traces traces = new Traces(hubProperties,"start");
        for (int i = 0; i < 1000; i++) {
            traces.add("" + i);
        }
        List<String> output = new ArrayList<>();
        traces.outputTraces(output::add);
        assertEquals(102, output.size());
    }

}