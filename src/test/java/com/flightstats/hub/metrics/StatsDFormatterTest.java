package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class StatsDFormatterTest {
    @Test
    public void testBuildCustomEvent_Event() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .hostTag("test_host")
                .build();

        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        Event event = statsDFormatter.buildCustomEvent("testTitle", "testText");

        assertEquals("testTitle", event.getTitle());
        assertEquals("testText", event.getText());
        assertEquals(Event.Priority.NORMAL.toString().toLowerCase(), event.getPriority());
        assertEquals(Event.AlertType.WARNING.toString().toLowerCase(), event.getAlertType());
        assertEquals("test_host", event.getHostname());
    }

    @Test
    public void testBuildCustomEvent_Error() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .build();
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        try {
            statsDFormatter.buildCustomEvent("", "");
        } catch (RuntimeException ex) {
            assertEquals(IllegalStateException.class, ex.getClass());
        }
    }

    @Test
    public void testFormatChannelTags_formattedTagOutput() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .hostTag("test_host")
                .build();

        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        String [] tags = statsDFormatter.formatChannelTags("testChannel", "testTagKey1:testTag1", "testTagKey2:testTag2");
        assertArrayEquals("arrays equals", Arrays.asList("testTagKey1:testTag1", "testTagKey2:testTag2", "channel:testChannel").toArray(), tags);
    }

    @Test
    public void testFormatChannelTags_Error() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .hostTag("test_host")
                .build();

        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        try {
            statsDFormatter.formatChannelTags("", "", "");
        } catch (RuntimeException ex) {
            assertEquals(IllegalStateException.class, ex.getClass());
        }
    }
}
