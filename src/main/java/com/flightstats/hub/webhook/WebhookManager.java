package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@Singleton
public class WebhookManager {

    private final static Logger logger = LoggerFactory.getLogger(WebhookManager.class);
    private final static String WATCHER_PATH = "/groupCallback/watcher";

    private final WatchManager watchManager;
    private final Dao<Webhook> webhookDao;
    private final LastContentPath lastContentPath;
    private final ActiveWebhooks activeWebhooks;
    private final CuratorCluster hubCluster;
    private final WebhookError webhookError;
    private final WebhookContentPathSet webhookInProcess;
    private final LocalWebhookManager localWebhookManager;
    private final Client client = RestClient.createClient(5, 15, true, true);

    @Inject
    public WebhookManager(WatchManager watchManager,
                          @Named("Webhook") Dao<Webhook> webhookDao,
                          LastContentPath lastContentPath,
                          ActiveWebhooks activeWebhooks,
                          @Named("HubCluster") CuratorCluster hubCluster,
                          WebhookError webhookError,
                          WebhookContentPathSet webhookInProcess,
                          LocalWebhookManager localWebhookManager)
    {
        this.watchManager = watchManager;
        this.webhookDao = webhookDao;
        this.lastContentPath = lastContentPath;
        this.activeWebhooks = activeWebhooks;
        this.hubCluster = hubCluster;
        this.webhookError = webhookError;
        this.webhookInProcess = webhookInProcess;
        this.localWebhookManager = localWebhookManager;

        HubServices.register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        HubServices.register(new WebhookScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void start() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                manageWebhooks(true);
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        manageWebhooks(false);
    }

    private synchronized void manageWebhooks(boolean useCache) {
        Set<Webhook> daoWebhooks = new HashSet<>(webhookDao.getAll(useCache));
        for (Webhook daoWebhook : daoWebhooks) {
            manageWebhook(daoWebhook, false);
        }
    }

    void notifyWatchers(Webhook webhook) {
        manageWebhook(webhook, true);
    }

    private void manageWebhook(Webhook daoWebhook, boolean webhookChanged) {
        if (daoWebhook.getTagUrl() != null && !daoWebhook.getTagUrl().isEmpty()) {
            // tag webhooks are not processed like normal webhooks.
            // they are used as prototype definitions for new webhooks added
            // automatically when a new/existing channel is assigned a tag that is
            // associated with a tag webhook
            return;
        }
        String name = daoWebhook.getName();
        if (activeWebhooks.getServers().contains(name)) {
            logger.debug("found existing v2 webhook {}", name);
            List<String> servers = new ArrayList<>(activeWebhooks.getServers(name));
            if (servers.size() >= 2) {
                logger.warn("found multiple servers! {}", servers);
                Collections.shuffle(servers);
                for (int i = 1; i < servers.size(); i++) {
                    callOneDelete(name, servers.get(i));
                }
            }
            if (servers.isEmpty()) {
                callOneRun(name, getOrderedServers());
            } else if (webhookChanged) {
                callOneRun(name, servers);
            }
        } else {
            logger.debug("found new v2 webhook {}", name);
            callOneRun(name, getOrderedServers());
        }
    }

    /**
     * We want this to return this list in order from fewest to most
     */
    private Collection<String> getOrderedServers() {
        TreeMap<Integer, String> orderedServers = new TreeMap<>();
        List<String> servers = hubCluster.getRandomServers();
        for (String server : servers) {
            int count = get(server + "/internal/webhook/count");
            orderedServers.put(count, server);
        }
        if (orderedServers.isEmpty()) {
            return servers;
        }
        return orderedServers.values();
    }

    private int get(String url) {
        ClientResponse response = null;
        String hubUrl = HubHost.getScheme() + url;
        try {
            logger.info("calling {}", hubUrl);
            response = client.resource(hubUrl).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
                return Integer.parseInt(response.getEntity(String.class));
            } else {
                logger.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            logger.warn("unable to get " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return 0;
    }

    private void callAllDelete(String name, Collection<String> servers) {
        for (String server : servers) {
            callOneDelete(name, server);
        }
    }

    private void callOneDelete(String name, String server) {
        put(server + "/internal/webhook/delete/" + name);
    }

    private void callOneRun(String name, Collection<String> servers) {
        for (String server : servers) {
            if (put(server + "/internal/webhook/run/" + name)) break;
        }
    }

    private boolean put(String url) {
        String hubUrl = HubHost.getScheme() + url;
        ClientResponse response = null;
        try {
            logger.info("calling {}", hubUrl);
            response = client.resource(hubUrl).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
                return true;
            } else {
                logger.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            logger.warn("unable to put " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return false;
    }

    private void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void delete(String name) {
        callAllDelete(name, activeWebhooks.getServers(name));
        lastContentPath.delete(name, WebhookLeader.WEBHOOK_LAST_COMPLETED);
    }

    public void getStatus(Webhook webhook, WebhookStatus.WebhookStatusBuilder statusBuilder) {
        statusBuilder.lastCompleted(lastContentPath.get(webhook.getName(), WebhookStrategy.createContentPath(webhook), WebhookLeader.WEBHOOK_LAST_COMPLETED));
        try {
            statusBuilder.errors(webhookError.get(webhook.getName()));
            ArrayList<ContentPath> inFlight = new ArrayList<>(new TreeSet<>(webhookInProcess.getSet(webhook.getName(), WebhookStrategy.createContentPath(webhook))));
            statusBuilder.inFlight(inFlight);
        } catch (Exception e) {
            logger.warn("unable to get status " + webhook.getName(), e);
            statusBuilder.errors(Collections.emptyList());
            statusBuilder.inFlight(Collections.emptyList());
        }
    }

    private class WebhookIdleService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            localWebhookManager.stopAllLocal();
            notifyWatchers();
        }

    }

    private class WebhookScheduledService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            manageWebhooks(false);
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.MINUTES);
        }
    }
}
