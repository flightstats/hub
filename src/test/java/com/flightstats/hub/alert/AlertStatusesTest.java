package com.flightstats.hub.alert;

import org.junit.Test;


public class AlertStatusesTest {

    @Test
    public void testNothing() {
        //todo - gfm - 2/25/16 - what should this be doing?
    }
/*    @Before
    public void setUp() throws Exception {
        HubProperties.setProperty("app.url", "http://localhost:4567/");
    }

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
        AlertStatuses.create();
        assertTrue(created[0]);
    }

    @Test
    public void testLatestNone() {
        get("/channel/testStatusLatestNone/latest", (req, res) -> {
            res.status(400);
            return "";
        });
        HubProperties.setProperty("alert.channel.status", "testStatusLatestNone");

        Map<String, AlertStatus> latest = AlertStatuses.getLatestMap();
        assertTrue(latest.isEmpty());
    }

    @Test
    public void testLatestKey() {
        HubProperties.setProperty("alert.channel.status", "testLatestKey");
        get("/channel/testLatestKey/latest", (req, res) -> {
            res.header("location", "http://localhost:4567/channel/testLatestKey/2015/06/09/16/23/33/202/bo0CTQ");
            return "";
        });

        assertEquals(new ContentKey(2015, 6, 9, 16, 23, 33, 202, "bo0CTQ"), AlertStatuses.getLatestKey().get());
    }

    @Test
    public void testLatestKeyMissing() {
        HubProperties.setProperty("alert.channel.status", "testLatestKeyMissing");
        get("/channel/testLatestKeyMissing/latest", (req, res) -> {
            res.status(400);
            return "";
        });

        assertFalse(AlertStatuses.getLatestKey().isPresent());
    }*/
}