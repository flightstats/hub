package com.flightstats.hub.alert;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.flightstats.hub.test.SparkUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AlertConfigsTest {

    private String hubAppUrl = "http://localhost:4567/";

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
        AlertConfigs alertConfigs = new AlertConfigs(hubAppUrl);
        alertConfigs.create();
        assertTrue(created[0]);
    }

    @Test
    public void testLatestNone() {
        get("/channel/testLatestNone/latest", (req, res) -> {
            res.status(400);
            return "";
        });
        HubProperties.setProperty("alert.channel.config", "testLatestNone");
        AlertConfigs alertConfigs = new AlertConfigs(hubAppUrl);
        List<AlertConfig> latest = alertConfigs.getLatest();
        assertTrue(latest.isEmpty());
    }

    @Test
    public void testLatestConfigs() throws IOException {
        URL resource = AlertRunnerTest.class.getResource("/alertConfig.json");
        String configString = IOUtils.toString(resource);
        get("/channel/testLatestConfigs/latest", (req, res) -> configString);
        HubProperties.setProperty("alert.channel.config", "testLatestConfigs");
        AlertConfigs alertConfigs = new AlertConfigs(hubAppUrl);
        List<AlertConfig> latest = alertConfigs.getLatest();
        assertEquals(7, latest.size());
        assertEquals("greaterThanName", latest.get(0).getName());
        assertEquals(AlertConfig.AlertType.CHANNEL, latest.get(0).getType());
        assertEquals("groupAlert1", latest.get(5).getName());
        assertEquals(AlertConfig.AlertType.GROUP, latest.get(5).getType());
    }
}