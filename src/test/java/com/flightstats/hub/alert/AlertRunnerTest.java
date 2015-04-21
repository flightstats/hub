package com.flightstats.hub.alert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AlertRunnerTest {

    private final static Logger logger = LoggerFactory.getLogger(AlertRunnerTest.class);

    @Test
    public void testParsing() throws IOException {

        URL resource = AlertRunnerTest.class.getResource("/test-config.json");

        String configString = IOUtils.toString(resource);
        logger.info("config {}", configString);
        List<AlertConfig> alertConfigs = AlertRunner.readConfig(configString, "app-url");
        assertEquals(5, alertConfigs.size());
        AlertConfig alertConfig = alertConfigs.get(0);
        assertNotNull(alertConfig);
        assertEquals(10, alertConfig.getTimeWindowMinutes());


    }

}