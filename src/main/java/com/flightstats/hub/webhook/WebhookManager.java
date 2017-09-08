package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.Cluster;
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
import java.util.stream.Collectors;

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
    @Named("HubCluster")
    private Cluster hubCluster;

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
        manageWebhooks();
    }

    private synchronized void manageWebhooks() {
        //todo - gfm - do we really need to run this N(odes) times for all webhooks?
        Set<String> activeV1Webhooks = activeWebhooks.getV1();
        Set<String> activeV2Webhooks = activeWebhooks.getV2();
        Set<Webhook> daoWebhooks = new HashSet<>(webhookDao.getAll(false));

        for (Webhook daoWebhook : daoWebhooks) {
            String name = daoWebhook.getName();
            if (activeV1Webhooks.contains(name)) {
                //if is in v1 ZK, leave it alone ...
                //todo - gfm - this can go away, eventually
                logger.info("found v1 webhook {}", name);
            } else if (activeV2Webhooks.contains(name)) {
                logger.debug("found existing v2 webhook {}", name);
                //todo - gfm - what happens if we have more than one?  call stop & start?
                Set<String> v2Servers = activeWebhooks.getV2Servers(name);
                if (v2Servers.isEmpty()) {
                    startOne(name);
                } else {
                    callOneServer(name, "run", v2Servers);
                }
            } else {
                logger.debug("found new v2 webhook {}", name);
                startOne(name);
            }
        }

        Set<String> daoNames = daoWebhooks.stream()
                .map(Webhook::getName)
                .collect(Collectors.toSet());
        activeV2Webhooks.removeAll(daoNames);
        for (String orphanedV2 : activeV2Webhooks) {
            delete(orphanedV2);
        }
    }

    private void startOne(String name) {
        List<String> servers = new ArrayList<>(hubCluster.getAllServers());
        Collections.shuffle(servers);
        callOneServer(name, "run", servers);
    }

    //todo - gfm - consolidate callAllServers & callOneServer
    private void callAllServers(String name, String method, Collection<String> servers) {
        for (String server : servers) {
            String url = HubHost.getScheme() + server + "/internal/webhook/" + method + "/" + name;
            logger.info("calling {}", url);
            ClientResponse response = client.resource(url).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
            } else {
                logger.warn("unexpected response {}", response);
            }
        }
    }

    private void callOneServer(String name, String method, Collection<String> servers) {
        for (String server : servers) {
            String url = HubHost.getScheme() + server + "/internal/webhook/" + method + "/" + name;
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

    //todo - gfm - it would be nice if notify for a single change did not trigger calls about all webhooks
    void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void delete(String name) {
        callAllServers(name, "delete", activeWebhooks.getV2Servers(name));
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
            //todo - gfm - hmmm
            return Scheduler.newFixedRateSchedule(1, 2, TimeUnit.MINUTES);
        }
    }
}
