package com.flightstats.hub.alert;

import org.junit.After;
import org.junit.Test;

import static spark.SparkBase.stop;

public class AlertCheckerTest {

    @After
    public void tearDown() throws Exception {
        stop();
    }

    @Test
    public void testNothing() {
        //todo
    }

   /* @Test
    public void testNegative() throws Exception {
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("load_test_1")
                .hubDomain("http://hub-v2.svc.dev/")
                .timeWindowMinutes(2)
                .name("testSimple")
                .operator("<")
                .threshold(100)
                .build();


        AlertChecker alertChecker = new AlertChecker(alertConfig);
        alertChecker.start();
        assertFalse(alertChecker.checkForAlert());
        alertChecker.update();
        assertFalse(alertChecker.checkForAlert());
    }

    @Test
    public void testPositive() throws Exception {
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("load_test_1")
                .hubDomain("http://hub-v2.svc.dev/")
                .timeWindowMinutes(5)
                .name("testSimple")
                .operator(">")
                .threshold(100)
                .build();
        AlertChecker alertChecker = new AlertChecker(alertConfig);
        alertChecker.start();
        assertTrue(alertChecker.checkForAlert());
    }*/
}