package com.flightstats.hub.alert;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class AlertRunner implements Leader {

    private final static Logger logger = LoggerFactory.getLogger(AlertRunner.class);
    private final static ObjectMapper mapper = new ObjectMapper();
    private final int sleepPeriod;
    private final String hubAppUrl;
    private final ExecutorService threadPool;
    private final String alertChannelName;

    private CuratorFramework curator;
    private Client client;
    private CuratorLeader leader;
    private final Map<AlertConfig, AlertChecker> configCheckerMap = new HashMap<>();

    @Inject
    public AlertRunner(CuratorFramework curator, Client client) {
        this.curator = curator;
        this.client = client;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AlertRunner-%d").build();
        threadPool = Executors.newFixedThreadPool(20, threadFactory);
        hubAppUrl = HubProperties.getProperty("app.url", "");
        sleepPeriod = HubProperties.getProperty("alert.sleep.millis", 60 * 1000);
        alertChannelName = HubProperties.getProperty("alert.channel.config", "zomboAlertsConfig");
        if (HubProperties.getProperty("alert.run", true)) {
            logger.info("starting with url {} {} {} ", hubAppUrl, sleepPeriod, alertChannelName);
            HubServices.register(new AlertRunnerService(), HubServices.TYPE.POST_START);
        } else {
            logger.warn("AlertRunner not running");
        }
    }

    @Override
    public double keepLeadershipRate() {
        return 0.99;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {

        while (hasLeadership.get()) {
            long start = System.currentTimeMillis();
            ClientResponse response = client.resource(hubAppUrl + "channel/" + alertChannelName + "/latest").get(ClientResponse.class);
            if (response.getStatus() >= 400) {
                logger.warn("unable to get latest from {} {}", alertChannelName, response);
                Sleeper.sleep(sleepPeriod);
                return;
            }
            List<AlertConfig> currentConfigs = readConfig(response.getEntity(String.class), hubAppUrl);
            Set<AlertConfig> alertsToStop = new HashSet<>(configCheckerMap.keySet());
            for (AlertConfig currentConfig : currentConfigs) {
                if (configCheckerMap.containsKey(currentConfig)) {
                    alertsToStop.remove(currentConfig);
                    AlertChecker alertChecker = configCheckerMap.get(currentConfig);
                    threadPool.submit(alertChecker::update);
                } else {
                    logger.info("found new or changed alert {}", currentConfig);
                    AlertChecker alertChecker = new AlertChecker(currentConfig);
                    configCheckerMap.put(currentConfig, alertChecker);
                    alertChecker.start();
                }
            }

            for (AlertConfig alertConfig : alertsToStop) {
                logger.info("removing alert {}", alertConfig);
                configCheckerMap.remove(alertConfig);
            }

            //todo - gfm - 4/8/15 - save off status to the hub

            doSleep(start);
        }

    }

    private void doSleep(long start) {
        long time = System.currentTimeMillis() - start;
        if (time < sleepPeriod) {
            long sleepMillis = sleepPeriod - time;
            logger.debug("sleeping for {}", sleepMillis);
            Sleeper.sleep(sleepMillis);
        } else {
            logger.warn("processing took too long {}", time);
        }
    }

    static List<AlertConfig> readConfig(String config, String hubAppUrl) {
        List<AlertConfig> alertConfigs = new ArrayList<>();
        try {
            JsonNode insertAlerts = mapper.readTree(config).get("insertAlerts");
            Iterator<Map.Entry<String, JsonNode>> fields = insertAlerts.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                AlertConfig alertConfig = AlertConfig.fromJson(entry.getKey(), hubAppUrl, entry.getValue().toString());
                alertConfigs.add(alertConfig);
            }
        } catch (IOException e) {
            logger.warn("unable to parse", e);
            throw new RuntimeException(e);
        }
        return alertConfigs;
    }

    private class AlertRunnerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            leader.close();
            threadPool.shutdown();
        }
    }

    private void start() {
        leader = new CuratorLeader("/AlertRunner", this, curator);
        leader.start();
    }
}
