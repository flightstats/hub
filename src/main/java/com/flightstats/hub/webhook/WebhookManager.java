package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class WebhookManager {

    private final static Logger logger = LoggerFactory.getLogger(WebhookManager.class);

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    @Inject
    private WatchManager watchManager;
    @Inject
    @Named("Webhook")
    private Dao<Webhook> webhookDao;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private ActiveWebhooks activeWebhooks;

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster hubCluster;

    @Inject
    private WebhookError webhookError;
    @Inject
    private WebhookContentPathSet webhookInProcess;

    private final Client client = RestClient.createClient(5, 15, true, true);

    @Inject
    public WebhookManager() {
        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        register(new WebhookScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
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
        //todo - gfm - once we are past v1 webhooks, we can replace watchManager with addRemovalListener
        //hubCluster.addRemovalListener(event -> manageWebhooks());
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
        String name = daoWebhook.getName();
        if (activeWebhooks.getV1().contains(name)) {
            //if is in v1 ZK, leave it alone ...
            //todo - gfm - this can go away, eventually
            logger.info("found v1 webhook {}", name);
        } else if (activeWebhooks.getV2().contains(name)) {
            logger.debug("found existing v2 webhook {}", name);
            Collection<String> v2Servers = activeWebhooks.getV2Servers(name);
            if (v2Servers.isEmpty()) {
                callOneRun(name, hubCluster.getRandomServers());
            } else if (webhookChanged) {
                callOneRun(name, v2Servers);
            }
        } else {
            logger.debug("found new v2 webhook {}", name);
            callOneRun(name, hubCluster.getRandomServers());
        }
    }

    private void callAllDelete(String name, Collection<String> servers) {
        for (String server : servers) {
            String url = HubHost.getScheme() + server + "/internal/webhook/delete/" + name;
            logger.info("calling {}", url);
            ClientResponse response = client.resource(url).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
            } else {
                logger.warn("unexpected response {}", response);
            }
        }
    }

    private void callOneRun(String name, Collection<String> servers) {
        for (String server : servers) {
            String url = HubHost.getScheme() + server + "/internal/webhook/run/" + name;
            logger.info("calling {}", url);
            ClientResponse response = client.resource(url).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
                break;
            } else {
                logger.warn("unexpected response {}", response);
            }
        }
    }

    private void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void delete(String name) {
        callAllDelete(name, activeWebhooks.getV2Servers(name));
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
            HubProvider.getInstance(LocalWebhookManager.class).stopAllLocal();
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
            /*
            this is only needed in the case where a hub server crashes, and we are not notified
            //todo - gfm - what we really want is to monitor the hub cluster group, only trigger on removal
             */
            return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.MINUTES);
        }
    }
}
