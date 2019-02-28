package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DataDogWhitelistProviderTest {

    @Test
    public void testProvider_noValues() {
        DataDogWhitelistProvider dataDogWhitelistProvider = new DataDogWhitelistProvider();
        DataDogWhitelist dataDogWhitelist = dataDogWhitelistProvider.get();
        assertEquals(dataDogWhitelist.getClass(), DataDogWhitelist.class);
        assertArrayEquals(dataDogWhitelist.getWhitelist().toArray(), Collections.singleton("").toArray());
    }

    @Test
    public void testProvider() {
        HubProperties.setProperty("metrics.filter.include.patterns", "test1,test2,test3,test4,test5,test6");
        DataDogWhitelistProvider dataDogWhitelistProvider = new DataDogWhitelistProvider();
        DataDogWhitelist dataDogWhitelist = dataDogWhitelistProvider.get();
        List<String> expected = Arrays.asList("test1" ,"test2", "test3", "test4", "test5", "test6");
        assertTrue("has values test1,test2,test3,test4,test5,test6", dataDogWhitelist.getWhitelist().containsAll(expected));
    }
}
