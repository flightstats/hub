package com.flightstats.hub.config.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;

import java.io.IOException;

@Slf4j
public class ZooKeeperTestServer {

    private static TestingServer testingServer;

    public static void start() throws Exception {
        if (testingServer == null) {
            log.info("Zookeeper starting in test mode.");
            testingServer = new TestingServer(2181);
        } else {
            log.warn("Zookeeper already running in test mode.");
        }
    }

    public static void stop() throws IOException {
        if (testingServer != null) {
            log.info("Stopping zookeeper test server");
            testingServer.stop();
        }
    }

    public static void restart() throws Exception {
        if (testingServer != null) {
            log.info("Restarting zookeeper test server");
            testingServer.restart();
        }
    }
}