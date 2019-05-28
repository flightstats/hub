package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.MetricsProperties;
import com.timgroup.statsd.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsDFormatterTest {

    @Mock
    private MetricsProperties metricsProperties;
    private StatsDFormatter statsDFormatter;

    @BeforeEach
    void setup() {
        statsDFormatter = new StatsDFormatter(metricsProperties);
    }

    @Test
    void testBuildCustomEvent_Event() {
        when(metricsProperties.getHostTag()).thenReturn("test_host");
        Event event = statsDFormatter.buildCustomEvent("testTitle", "testText");

        assertEquals("testTitle", event.getTitle());
        assertEquals("testText", event.getText());
        assertEquals(Event.Priority.NORMAL.toString().toLowerCase(), event.getPriority());
        assertEquals(Event.AlertType.WARNING.toString().toLowerCase(), event.getAlertType());
        assertEquals("test_host", event.getHostname());
    }

    @Test
    void testBuildCustomEvent_Error() {
        Exception exception = assertThrows(IllegalStateException.class, () -> statsDFormatter.buildCustomEvent("", ""));
        assertEquals("event title must be set", exception.getMessage());
    }

    @Test
    void testFormatChannelTags_formattedTagOutput() {
        String[] tags = statsDFormatter.formatChannelTags("testChannel", "testTagKey1:testTag1", "testTagKey2:testTag2");
        assertArrayEquals(Arrays.asList("testTagKey1:testTag1", "testTagKey2:testTag2", "channel:testChannel").toArray(), tags);
    }
}
