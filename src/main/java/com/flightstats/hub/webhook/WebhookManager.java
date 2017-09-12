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
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private Provider<WebhookLeader> v2Provider;
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

    private Map<String, WebhookLeader> localLeaders = new ConcurrentHashMap<>();
    private final Client client = RestClient.createClient(5, 15, true, true);
    private final KeyLockManager lockManager;

    @Inject
    public WebhookManager() {
        lockManager = KeyLockManagers.newLock(1, TimeUnit.SECONDS);
        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        register(new WebhookScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void start() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                manageWebhooks();
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        //todo - gfm - once we are past v1 webhooks, we can replace watchManager with addRemovalListener
        //hubCluster.addRemovalListener(event -> manageWebhooks());
        manageWebhooks();
    }

    private synchronized void manageWebhooks() {
        Set<Webhook> daoWebhooks = new HashSet<>(webhookDao.getAll(false));
        for (Webhook daoWebhook : daoWebhooks) {
            manageWebhook(daoWebhook);
        }
    }

    void notifyWatchers(Webhook webhook) {
        manageWebhook(webhook);
    }

    private void manageWebhook(Webhook daoWebhook) {
        String name = daoWebhook.getName();
        if (activeWebhooks.getV1().contains(name)) {
            //if is in v1 ZK, leave it alone ...
            //todo - gfm - this can go away, eventually
            logger.info("found v1 webhook {}", name);
        } else if (activeWebhooks.getV2().contains(name)) {
            logger.debug("found existing v2 webhook {}", name);
            Collection<String> v2Servers = activeWebhooks.getV2Servers(name);
            if (v2Servers.isEmpty()) {
                v2Servers = hubCluster.getRandomServers();
            }
            callOneRun(name, v2Servers);
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

    boolean ensureRunning(String name) {
        return lockManager.executeLocked(name, () -> ensureRunningWithLock(name));
    }

    private boolean ensureRunningWithLock(String name) {
        Webhook daoWebhook = webhookDao.get(name);
        logger.info("ensureRunning {}", daoWebhook);
        if (localLeaders.containsKey(name)) {
            logger.info("checking for change {}", name);
            WebhookLeader webhookLeader = localLeaders.get(name);
            Webhook runningWebhook = webhookLeader.getWebhook();
            if (!runningWebhook.isChanged(daoWebhook)) {
                return true;
            }
            logger.info("webhook has changed {} to {}", runningWebhook, daoWebhook);
            stopLocal(name, false);
        }
        logger.info("starting {}", name);
        return startLocal(daoWebhook);
    }

    private boolean startLocal(Webhook daoWebhook) {
        WebhookLeader webhookLeader = v2Provider.get();
        boolean hasLeadership = webhookLeader.tryLeadership(daoWebhook);
        if (hasLeadership) {
            localLeaders.put(daoWebhook.getName(), webhookLeader);
        }
        return hasLeadership;
    }

    void stopLocal(String name, boolean delete) {
        logger.info("stop {} {}", name, delete);
        if (localLeaders.containsKey(name)) {
            logger.info("stopping local {}", name);
            localLeaders.get(name).exit(delete);
            localLeaders.remove(name);
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
            for (String name : localLeaders.keySet()) {
                stopLocal(name, false);
            }
            notifyWatchers();
        }

    }

    private class WebhookScheduledService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            //todo - gfm -  could this just check for empty locks ...
            manageWebhooks();
        }

        @Override
        protected Scheduler scheduler() {
            /*
            this is only needed in the case where a hub server crashes, and we are not notified
            //todo - gfm - what we really want is to monitor the hub cluster group, only trigger on removal
             */
            return Scheduler.newFixedRateSchedule(1, 2, TimeUnit.MINUTES);
        }
    }
}
