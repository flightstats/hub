package com.flightstats.hub.dao;

import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.time.TimeService;
import org.junit.jupiter.api.Test;

import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class ChannelServiceTest {

    private final ChannelService channelService = new ChannelService(
            mock(ContentService.class),
            mock(Dao.class),
            mock(Provider.class),
            mock(WatchManager.class),
            mock(ClusterCacheDao.class),
            mock(InFlightService.class),
            mock(TimeService.class),
            mock(StatsdReporter.class),
            mock(ContentRetriever.class),
            mock(ContentProperties.class)
    );

    @Test
    public void shouldRemoveCreationDateWhenPresent() {
        String inputJson = "{\"creationDate\": \"2024-10-24\", \"secondaryMetricsReporting\": \"false\"}";
        String expectedJson = "{\"allowZeroBytes\":\"false\"}";
        String result = channelService.handleCreationDate(inputJson);
        assertEquals(expectedJson, result);
    }

    @Test
    public void shouldReturnOriginalJsonWhenCreationDateNotPresent() {
        String inputJson = "{\"secondaryMetricsReporting\": \"false\"}";
        String expectedJson = "{\"allowZeroBytes\":\"true\"}";
        String result = channelService.handleCreationDate(inputJson);
        assertEquals(expectedJson, result);
    }

    @Test
    public void shouldReturnEmptyJsonWhenInputIsEmpty() {
        String inputJson = "";
        String expectedJson = "";
        String result = channelService.handleCreationDate(inputJson);
        assertEquals(expectedJson, result);
    }

    @Test
    public void shouldReturnNullWhenInputIsNull() {
        String inputJson = null;
        String result = channelService.handleCreationDate(inputJson);
        assertNull(result);
    }
}
