package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;

public class DataDogMetricsServiceTest {

    @Test
    public void testChannelTag() {
        HubProperties hubProperties = mock(HubProperties.class);
        DataDog dataDog = mock(DataDog.class);
        DataDogMetricsService metricsService = new DataDogMetricsService(dataDog, hubProperties);
        String[] tags = metricsService.addChannelTag("stuff", "name:one");
        assertArrayEquals(new String[]{"name:one", "channel:stuff"}, tags);
    }

    @Test
    public void testChannelTagThree() {
        HubProperties hubProperties = mock(HubProperties.class);
        DataDog dataDog = mock(DataDog.class);
        DataDogMetricsService metricsService = new DataDogMetricsService(dataDog, hubProperties);
        String[] input = new String[]{"name:one", "name:two", "name:3"};
        String[] tags = metricsService.addChannelTag("stuff", input);
        List<String> strings = new ArrayList<>(Arrays.asList(input));
        strings.add("channel:stuff");
        assertArrayEquals(strings.toArray(new String[strings.size()]), tags);
    }

    @Test
    public void testChannelNone() {
        HubProperties hubProperties = mock(HubProperties.class);
        DataDog dataDog = mock(DataDog.class);
        DataDogMetricsService metricsService = new DataDogMetricsService(dataDog, hubProperties);
        String[] tags = metricsService.addChannelTag("stuff");
        assertArrayEquals(new String[]{"channel:stuff"}, tags);
    }

}
