package com.flightstats.hub.alert;


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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class AlertRunner implements Leader {

    private final static Logger logger = LoggerFactory.getLogger(AlertRunner.class);

    private final int sleepPeriod;
    private final String hubAppUrl;
    private final ExecutorService threadPool;
    private final AlertConfigs alertConfigs;
    private final AlertStatuses alertStatuses;

    private CuratorFramework curator;
    private static final Client client = RestClient.createClient(15, 60);
    private CuratorLeader leader;


    @Inject
    public AlertRunner(CuratorFramework curator) {
        this.curator = curator;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AlertRunner-%d").build();
        threadPool = Executors.newFixedThreadPool(20, threadFactory);
        hubAppUrl = StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
        sleepPeriod = HubProperties.getProperty("alert.sleep.millis", 60 * 1000);
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
        while (hasLeadership.get()) {
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
        List<AlertConfig> alertConfigsLatest = alertConfigs.getLatest();
        Map<String, AlertStatus> existingAlertStatus = alertStatuses.getLatest();
        List<Future<AlertStatus>> futures = new ArrayList<>();
        for (AlertConfig alertConfig : alertConfigsLatest) {
            if (existingAlertStatus.containsKey(alertConfig.getName())) {
                AlertStatus alertStatus = existingAlertStatus.get(alertConfig.getName());
                futures.add(threadPool.submit(new AlertUpdater(alertConfig, alertStatus)));
            } else {
                futures.add(threadPool.submit(new AlertUpdater(alertConfig)));
            }
        }
        Map<String, AlertStatus> updatedAlertStatus = new HashMap<>();
        for (Future<AlertStatus> future : futures) {
            try {
                AlertStatus alertStatus = future.get();
                updatedAlertStatus.put(alertStatus.getName(), alertStatus);
            } catch (Exception e) {
                logger.warn("unable to get status", e);

            }
        }
        alertStatuses.saveStatus(updatedAlertStatus);
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

    private class AlertRunnerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            createChannels();
            start();
        }

        @Override
        protected void shutDown() throws Exception {
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
                    .put("{\"ttlDays\":14, \"description:\"alerts to be sent and confirmations\"}");
        } catch (Exception e) {
            logger.warn("hate filled donut", e);
        }
    }

    private void start() {
        leader = new CuratorLeader("/AlertRunner", this, curator);
        leader.start();
    }
}
