package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsDFormatterTest {
    @Test
    void testBuildCustomEvent_Event() {
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
    void testBuildCustomEvent_Error() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .build();
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        Exception exception = assertThrows(IllegalStateException.class, () -> statsDFormatter.buildCustomEvent("", ""));
        assertEquals("event title must be set", exception.getMessage());
    }

    @Test
    void testFormatChannelTags_formattedTagOutput() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .hostTag("test_host")
                .build();

        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        String [] tags = statsDFormatter.formatChannelTags("testChannel", "testTagKey1:testTag1", "testTagKey2:testTag2");
        assertArrayEquals(Arrays.asList("testTagKey1:testTag1", "testTagKey2:testTag2", "channel:testChannel").toArray(), tags);
    }
}
