package com.flightstats.hub.metrics;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.Test;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StatsDFilterTest {

    @Test
    public void testStatsDFilterGetAllClients_twoNoOpClients() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        assertEquals(2, statsDFilter.getAllClients().size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(1).getClass());
    }

    @Test
    public void testStatsDFilterGetAllClients_twoCustomClients() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getAllClients().size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getAllClients().get(1).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_throwsOnNoWhiteList() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist
                .builder()
                .build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        try {
            statsDFilter.getFilteredClients("testChannelName");
        } catch (Exception ex) {
            assertEquals(NullPointerException.class, ex.getClass());
        }
    }

    @Test
    public void testStatsDFilterGetFilteredClients_oneClient() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist
                .builder()
                .whitelist(Collections.singletonList(""))
                .build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        assertEquals(1, statsDFilter.getFilteredClients("testChannelName").size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist
                .builder()
                .whitelist(Collections.singletonList("testChannelName"))
                .build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        assertEquals(2, statsDFilter.getFilteredClients("testChannelName").size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_oneClientFiltered() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist
                .builder()
                .whitelist(Collections.singletonList("testChannelName"))
                .build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        assertEquals(1, statsDFilter.getFilteredClients("testChannelName2").size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_twoClientsSpecialChars() {
        DataDogWhitelist dataDogWhitelist = DataDogWhitelist
                .builder()
                .whitelist(Collections.singletonList("@_-(*&*"))
                .build();
        StatsDFilter statsDFilter = new StatsDFilter(dataDogWhitelist);
        assertEquals(2, statsDFilter.getFilteredClients("@_-(*&*").size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getAllClients().get(0).getClass());
    }


}
