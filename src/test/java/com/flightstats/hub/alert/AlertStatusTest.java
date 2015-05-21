package com.flightstats.hub.alert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;

public class AlertStatusTest {

    private final static Logger logger = LoggerFactory.getLogger(AlertStatusTest.class);

    @Test
    public void testLoad() throws IOException {
        URL resource = AlertRunnerTest.class.getResource("/alertStatus.json");

        String statusJson = IOUtils.toString(resource);

        Map<String, AlertStatus> alertStatusMap = AlertStatus.fromJson(statusJson);
        logger.info("parsed {}", alertStatusMap);

        assertEquals(4, alertStatusMap.size());
        AlertStatus v2Example = alertStatusMap.get("v2-example");
        assertNotNull(v2Example);
        assertTrue(v2Example.isAlert());
        assertEquals(3, v2Example.getHistory().size());
        assertEquals("http://hub/channel/zomboAlertsConfig/2015/05/20/22/57?stable=true", v2Example.getHistory().get(1).getHref());

        String cycledJson = AlertStatus.toJson(alertStatusMap);
        statusJson = statusJson.replaceAll("\\s+", "");
        assertEquals(statusJson, cycledJson);

    }

}