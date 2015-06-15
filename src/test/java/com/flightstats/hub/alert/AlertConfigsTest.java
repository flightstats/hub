package com.flightstats.hub.alert;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static com.flightstats.hub.test.SparkUtil.*;
import static org.junit.Assert.*;

public class AlertConfigsTest {

    @Before
    public void setUp() throws Exception {
        HubProperties.setProperty("app.url", "http://localhost:4567/");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stop();
        HubProperties.setProperty("alert.channel.config", "zomboAlertsConfig");
    }

    @Test
    public void testCreate() {
        final boolean[] created = {false};
        put("/channel/testCreate", (req, res) -> created[0] = true);
        HubProperties.setProperty("alert.channel.config", "testCreate");
        AlertConfigs.create();
        assertTrue(created[0]);
    }

    @Test
    public void testLatestNone() {
        get("/channel/testLatestNone/latest", (req, res) -> {
            res.status(400);
            return "";
        });
        HubProperties.setProperty("alert.channel.config", "testLatestNone");
        Map<String, AlertConfig> latest = AlertConfigs.getLatest();
        assertTrue(latest.isEmpty());
    }

    @Test
    public void testLatestConfigs() throws IOException {
        URL resource = AlertRunnerTest.class.getResource("/alertConfig.json");
        String configString = IOUtils.toString(resource);
        get("/channel/testLatestConfigs/latest", (req, res) -> configString);
        HubProperties.setProperty("alert.channel.config", "testLatestConfigs");
        Map<String, AlertConfig> latest = AlertConfigs.getLatest();
        assertEquals(7, latest.size());
        AlertConfig greaterThanName = latest.get("greaterThanName");
        assertTrue(greaterThanName.isChannelAlert());
        assertEquals("greaterThan", greaterThanName.getSource());

        AlertConfig greaterThanEqualName = latest.get("greaterThanEqualName");
        assertTrue(greaterThanEqualName.isChannelAlert());
        assertEquals("greaterThanEqual", greaterThanEqualName.getSource());

        //todo - gfm - 6/15/15 - source
        assertTrue(greaterThanName.isChannelAlert());
        assertFalse(latest.get("groupAlert1").isChannelAlert());
    }
}