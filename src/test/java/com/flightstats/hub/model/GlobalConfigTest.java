package com.flightstats.hub.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class GlobalConfigTest {

    private GlobalConfig globalConfig;

    @Before
    public void setUp() throws Exception {
        globalConfig = new GlobalConfig();
    }

    @Test
    public void testAppendMaster() {
        globalConfig.setMaster("abc");
        assertEquals("abc/", globalConfig.getMaster());
    }

    @Test
    public void testAppendSatellite() {
        globalConfig.addSatellite("abc");
        globalConfig.addSatellite("def");
        assertEquals("[abc/, def/]", globalConfig.getSatellites().toString());
    }

    @Test
    public void testAppendSatellites() {
        globalConfig.addSatellites(Arrays.asList("abc", "def"));
        globalConfig.addSatellite("def");
        assertEquals("[abc/, def/]", globalConfig.getSatellites().toString());
    }
}