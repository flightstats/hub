package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static spark.Spark.get;
import static spark.SparkBase.stop;

public class AlertCheckerMockTest {

    private final static Logger logger = LoggerFactory.getLogger(AlertCheckerMockTest.class);
    private final static ObjectMapper mapper = new ObjectMapper();
    private static boolean triggerAlert = false;
    private static int next = 1;

    @After
    public void tearDown() throws Exception {
        stop();
    }

    @Test
    public void testNegative() throws Exception {
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("load_test_1")
                .hubDomain("http://localhost:4567")
                .timeWindowMinutes(2)
                .name("testSimple")
                .operator("<")
                .threshold(4)
                .build();

        String base = "/channel/load_test_1/";
        get(base + "time/minute", (req, res) -> {
            res.redirect(base + "2015/03/02/22/25");
            return null;
        });
        get(base + "2015/03/02/22/:minute", AlertCheckerMockTest::handleMinute);

        next = 3;
        AlertChecker alertChecker = new AlertChecker(alertConfig);
        alertChecker.start();
        assertFalse(alertChecker.checkForAlert());

        next = 1;
        alertChecker.update();
        assertFalse(alertChecker.checkForAlert());

        next = 1;
        triggerAlert = true;
        alertChecker.update();
        assertTrue(alertChecker.checkForAlert());
    }

    private static Object handleMinute(Request request, Response response) {
        String minute = request.params("minute");
        String url = request.url();
        logger.info("url {} minute {}", url, minute);
        String hourUrl = StringUtils.removeEnd(url, minute);
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        links.putObject("self").put("href", url);

        links.putObject("previous").put("href", hourUrl + (Integer.parseInt(minute) - 1));
        ArrayNode uris = links.putArray("uris");
        if (!triggerAlert) {
            uris.add("one");
            uris.add("two");
        }
        if (next > 0) {
            next--;
            links.putObject("next").put("href", hourUrl + (Integer.parseInt(minute) + 1));
        }

        return root.toString();
    }

}