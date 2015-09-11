package com.flightstats.hub.group;

import com.flightstats.hub.test.SparkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinuteGroupStrategyTest {

    private final static Logger logger = LoggerFactory.getLogger(MinuteGroupStrategyTest.class);

    public static void main(String[] args) {

        SparkUtil.get("minute", (req, res) -> {
            logger.info("get {}", req.body());
            return "";
        });

        SparkUtil.post("minute", (req, res) -> {
            logger.info("post {}", req.body());
            return "";
        });
    }

}