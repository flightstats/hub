package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpokeServerTest {

    @Before
    public void setUp() throws Exception {
        HubProperties.setProperty("spoke.failure.count", "10");
        HubProperties.setProperty("spoke.failure.window.minutes", "1");
    }

    @Test
    public void testEligiblity() {
        SpokeServer spokeServer = new SpokeServer("A");
        assertTrue(spokeServer.isEligible());
        failures(spokeServer);
        assertTrue(spokeServer.isEligible());
        spokeServer.logFailure("straw");
        assertFalse(spokeServer.isEligible());
        assertFalse(spokeServer.isEligible());
    }

    private void failures(SpokeServer spokeServer) {
        for (int i = 0; i < 10; i++) {
            spokeServer.logFailure(" " + i);
        }
    }

    @Test
    public void testEligiblityOlder() {
        SpokeServer spokeServer = new SpokeServer("B");
        spokeServer.setStartTime(TimeUtil.now().minusSeconds(61));
        assertTrue(spokeServer.isEligible());
        failures(spokeServer);
        assertTrue(spokeServer.isEligible());
        spokeServer.logFailure("straw");
        assertTrue(spokeServer.isEligible());
        assertTrue(spokeServer.isEligible());
    }


}