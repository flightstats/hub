package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    private WebhookError webhookError;
    @Inject
    private WebhookContentPathSet webhookInProcess;

    @Inject
    private InternalWebhookClient webhookClient;

    @Inject
    private WebhookStateReaper webhookStateReaper;

    @Inject
    public WebhookManager() {
        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        register(new WebhookScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    @VisibleForTesting
    WebhookManager(WatchManager watchManager,
                   Dao<Webhook> webhookDao,
                   LastContentPath lastContentPath,
                   ActiveWebhooks activeWebhooks,
                   WebhookError webhookError,
                   WebhookContentPathSet webhookInProcess,
                   InternalWebhookClient webhookClient) {
        this.watchManager = watchManager;
        this.webhookDao = webhookDao;
        this.lastContentPath = lastContentPath;
        this.activeWebhooks = activeWebhooks;
        this.webhookError = webhookError;
        this.webhookInProcess = webhookInProcess;
        this.webhookClient = webhookClient;
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

    @VisibleForTesting
    void manageWebhook(Webhook daoWebhook, boolean webhookChanged) {
        if (daoWebhook.getTagUrl() != null && !daoWebhook.getTagUrl().isEmpty()) {
            // tag webhooks are not processed like normal webhooks.
            // they are used as prototype definitions for new webhooks added
            // automatically when a new/existing channel is assigned a tag that is
            // associated with a tag webhook
            return;
        }
        String name = daoWebhook.getName();
        if (activeWebhooks.isActiveWebhook(name)) {
            logger.debug("found existing v2 webhook {}", name);
            List<String> servers = new ArrayList<>(activeWebhooks.getServers(name));
            if (servers.size() >= 2) {
                logger.warn("found multiple servers! {}", servers);
                Collections.shuffle(servers);
                for (int i = 1; i < servers.size(); i++) {
                    webhookClient.remove(name, servers.get(i));
                }
            }
            if (servers.isEmpty()) {
                webhookClient.runOnServerWithFewestWebhooks(name);
            } else if (webhookChanged) {
                webhookClient.runOnOneServer(name, servers);
            }
        } else {
            logger.debug("found new v2 webhook {}", name);
            webhookClient.runOnServerWithFewestWebhooks(name);
        }
    }

    private void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void delete(String name) {
        webhookClient.remove(name, activeWebhooks.getServers(name));
        webhookStateReaper.delete(name);
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
            return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.MINUTES);
        }
    }
}
