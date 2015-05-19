package com.flightstats.hub.alert;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class AlertRunner {

    private final static Logger logger = LoggerFactory.getLogger(AlertRunner.class);
    private final static ObjectMapper mapper = new ObjectMapper();
    private final int sleepPeriod;
    private final String hubAppUrl;
    private final ExecutorService threadPool;
    private final String alertChannelName;
    private final String alertChannelStatus;

    private CuratorFramework curator;
    private static final Client client = RestClient.createClient(15, 60);
    private CuratorLeader leader;
    private final Map<AlertConfig, AlertChecker> configCheckerMap = new HashMap<>();
    private final AtomicBoolean run = new AtomicBoolean(true);

    @Inject
    public AlertRunner(CuratorFramework curator) {
        this.curator = curator;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AlertRunner-%d").build();
        threadPool = Executors.newFixedThreadPool(20, threadFactory);
        hubAppUrl = StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
        sleepPeriod = HubProperties.getProperty("alert.sleep.millis", 60 * 1000);
        alertChannelName = HubProperties.getProperty("alert.channel.config", "zomboAlertsConfig");
        alertChannelStatus = HubProperties.getProperty("alert.channel.status", "zomboAlertStatus");
        if (HubProperties.getProperty("alert.run", true)) {
            logger.info("starting with url {} {} {} ", hubAppUrl, sleepPeriod, alertChannelName);
            HubServices.register(new AlertRunnerService(), HubServices.TYPE.POST_START);

        } else {
            logger.warn("AlertRunner not running");
        }
    }

    public void run() {
        while (run.get()) {
            try {
                doWork();
            } catch (Exception e) {
                logger.warn("unable to procese", e);
            }

        }
    }

    private void doWork() {
        long start = System.currentTimeMillis();
        ClientResponse response = client.resource(hubAppUrl + "channel/" + alertChannelName + "/latest")
                .get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", alertChannelName, response);
            Sleeper.sleep(sleepPeriod);
        } else {
            saveStatus();

            List<AlertConfig> currentConfigs = readConfig(response.getEntity(String.class), hubAppUrl);
            Set<AlertConfig> alertsToStop = new HashSet<>(configCheckerMap.keySet());

            for (AlertConfig currentConfig : currentConfigs) {
                if (configCheckerMap.containsKey(currentConfig)) {
                    alertsToStop.remove(currentConfig);
                    AlertChecker alertChecker = configCheckerMap.get(currentConfig);
                    if (!alertChecker.inProcess()) {
                        threadPool.submit(alertChecker::update);
                    }
                } else {
                    logger.info("found new or changed alert {}", currentConfig);
                    AlertChecker alertChecker = new AlertChecker(currentConfig, curator);
                    configCheckerMap.put(currentConfig, alertChecker);
                    String name = currentConfig.getName();
                    threadPool.submit(alertChecker::start);
                }
            }

            for (AlertConfig alertConfig : alertsToStop) {
                logger.info("removing alert {}", alertConfig);
                configCheckerMap.remove(alertConfig);
            }
            doSleep(start);
        }
    }

    private void saveStatus() {
        ObjectNode status = mapper.createObjectNode();
        for (AlertChecker alertChecker : configCheckerMap.values()) {
            if (!alertChecker.inProcess()) {
                alertChecker.toJson(status);
            }
        }
        if (status.size() > 0) {
            String entity = status.toString();
            logger.info("saving status {}", entity);
            client.resource(hubAppUrl + "channel/" + alertChannelStatus)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(entity);
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
            createChannels();
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            run.set(false);
            leader.close();
            threadPool.shutdown();
        }
    }

    private void createChannels() {
        try {
            client.resource(hubAppUrl + "channel/" + alertChannelName)
                    .type(MediaType.APPLICATION_JSON)
                    .put("{'ttlDays':1000, 'description:'Configuration for hub alerts'}");
            client.resource(hubAppUrl + "channel/" + alertChannelStatus)
                    .type(MediaType.APPLICATION_JSON)
                    .put("{'ttlDays':7, 'description:'Status for hub alerts'}");
            String alertChannelEscalate = HubProperties.getProperty("alert.channel.escalate", "escalationAlerts");
            client.resource(hubAppUrl + "channel/" + alertChannelEscalate)
                    .type(MediaType.APPLICATION_JSON)
                    .put("{'ttlDays':14, 'description:'alerts to be sent and confirmations'}");
        } catch (Exception e) {
            logger.warn("hate filled donut", e);
        }
    }

    private void start() {
        Executors.newSingleThreadExecutor().submit(this::run);
    }
}
