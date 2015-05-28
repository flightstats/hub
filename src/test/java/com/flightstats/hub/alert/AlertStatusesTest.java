package com.flightstats.hub.alert;

import com.flightstats.hub.app.HubProperties;
import org.junit.AfterClass;
import org.junit.Test;

import static com.flightstats.hub.test.SparkUtil.*;
import static org.junit.Assert.assertTrue;


public class AlertStatusesTest {

    private String hubAppUrl = "http://localhost:4567/";

    @AfterClass
    public static void tearDown() throws Exception {
        stop();
        HubProperties.setProperty("alert.channel.status", "zomboAlertStatus");
    }

    @Test
    public void testCreate() {
        final boolean[] created = {false};
        put("/channel/testCreate", (req, res) -> created[0] = true);
        HubProperties.setProperty("alert.channel.status", "testCreate");
        AlertStatuses alertStatuses = new AlertStatuses(hubAppUrl);
        alertStatuses.create();
        assertTrue(created[0]);
    }

    @Test
    public void testLatestNone() {
        get("/channel/testStatusLatestNone/latest", (req, res) -> {
            res.status(400);
            return "";
        });
        HubProperties.setProperty("alert.status.config", "testStatusLatestNone");
        AlertStatuses alertStatuses = new AlertStatuses(hubAppUrl);
        //todo - gfm - 5/20/15 -
        /*List<AlertConfig> latest = alertStatuses.getLatest();
        assertTrue(latest.isEmpty());*/
    }
}