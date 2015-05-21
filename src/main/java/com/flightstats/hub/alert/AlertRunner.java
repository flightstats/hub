package com.flightstats.hub.alert;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
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
    private final String alertStatusName;
    private final AlertConfigs alertConfigs;

    private CuratorFramework curator;
    private static final Client client = RestClient.createClient(15, 60);
    private CuratorLeader leader;
    private final Map<AlertConfig, AlertChecker> configCheckerMap = new HashMap<>();
    private final AtomicBoolean run = new AtomicBoolean(true);
    private final AlertStatuses alertStatuses;

    @Inject
    public AlertRunner(CuratorFramework curator) {
        this.curator = curator;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AlertRunner-%d").build();
        threadPool = Executors.newFixedThreadPool(20, threadFactory);
        hubAppUrl = StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
        sleepPeriod = HubProperties.getProperty("alert.sleep.millis", 60 * 1000);
        alertStatusName = HubProperties.getProperty("alert.channel.status", "zomboAlertStatus");
        alertConfigs = new AlertConfigs(hubAppUrl, client);
        alertStatuses = new AlertStatuses(hubAppUrl, client);
        if (HubProperties.getProperty("alert.run", true)) {

            logger.info("starting with url {} {} ", hubAppUrl, sleepPeriod);
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

        /**
         * thread independent?
         *  get latest config
         *  get latest status
         * compare parsed config to parsed status
         *   update all extant alerts
         *      treat 120+ minutes as N hours, round up
         * wait for all updates
         *   update status
         * sleep for rest of minute, or none if took more than a minute
         */

        List<AlertConfig> alertConfigsLatest = alertConfigs.getLatest();
        alertStatuses.getLatest();


    }

    public void run() {
        while (run.get()) {
            try {
                doWork();
            } catch (Exception e) {
                logger.warn("unable to process", e);
            }

        }
    }

    private void doWork() {
        logger.info("doing work");
        long start = System.currentTimeMillis();

        //todo - gfm - 5/20/15 -
        //saveStatus();

        List<AlertConfig> currentConfigs = readConfig("nothing", hubAppUrl);
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


    private void doSleep(long start) {
        long time = System.currentTimeMillis() - start;
        if (time < sleepPeriod) {
            long sleepMillis = sleepPeriod - time;
            logger.info("sleeping for {}", sleepMillis);
            Sleeper.sleep(sleepMillis);
        } else {
            logger.warn("processing took too long {}", time);
        }
    }

    static List<AlertConfig> readConfig(String config, String hubAppUrl) {
        List<AlertConfig> alertConfigs = new ArrayList<>();
        //todo - gfm - 5/20/15 - delete method
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
            alertConfigs.create();
            alertStatuses.create();
            String alertChannelEscalate = HubProperties.getProperty("alert.channel.escalate", "escalationAlerts");
            client.resource(hubAppUrl + "channel/" + alertChannelEscalate)
                    .type(MediaType.APPLICATION_JSON)
                    .put("{'ttlDays':14, 'description:'alerts to be sent and confirmations'}");
        } catch (Exception e) {
            logger.warn("hate filled donut", e);
        }
    }

    private void start() {
        leader = new CuratorLeader("/AlertRunner", this, curator);
        leader.start();
    }
}
