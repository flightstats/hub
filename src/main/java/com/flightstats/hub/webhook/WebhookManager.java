package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.api.CuratorEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.register;
import static com.flightstats.hub.util.Constants.WEBHOOK_LAST_COMPLETED;

@Singleton
@Slf4j
public class WebhookManager {

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    private final LocalWebhookManager localWebhookManager;
    private final WebhookErrorService webhookErrorService;
    private final WebhookContentPathSet webhookContentPathSet;
    private final InternalWebhookClient webhookClient;
    private final WebhookStateReaper webhookStateReaper;
    private final LastContentPath lastContentPath;
    private final ActiveWebhooks activeWebhooks;
    private final WatchManager watchManager;
    private final Dao<Webhook> webhookDao;

    @Inject
    public WebhookManager(LocalWebhookManager localWebhookManager,
                          WebhookErrorService webhookErrorService,
                          WebhookContentPathSet webhookContentPathSet,
                          InternalWebhookClient webhookClient,
                          WebhookStateReaper webhookStateReaper,
                          LastContentPath lastContentPath,
                          ActiveWebhooks activeWebhooks,
                          WatchManager watchManager,
                          @Named("Webhook") Dao<Webhook> webhookDao) {
        this.localWebhookManager = localWebhookManager;
        this.webhookErrorService = webhookErrorService;
        this.webhookContentPathSet = webhookContentPathSet;
        this.webhookClient = webhookClient;
        this.webhookStateReaper = webhookStateReaper;
        this.lastContentPath = lastContentPath;
        this.activeWebhooks = activeWebhooks;
        this.watchManager = watchManager;
        this.webhookDao = webhookDao;

        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        register(new WebhookScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private void start() {
        log.info("starting");
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
        final Set<Webhook> daoWebhooks = new HashSet<>(webhookDao.getAll(useCache));
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
        final String name = daoWebhook.getName();
        if (activeWebhooks.isActiveWebhook(name)) {
            log.debug("found existing v2 webhook {}", name);
            final List<String> servers = new ArrayList<>(activeWebhooks.getServers(name));
            if (servers.size() >= 2) {
                log.warn("found multiple servers! {}", servers);
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
            log.debug("found new v2 webhook {}", name);
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
        statusBuilder.lastCompleted(lastContentPath.get(webhook.getName(), WebhookStrategy.createContentPath(webhook), WEBHOOK_LAST_COMPLETED));
        try {
            statusBuilder.errors(webhookErrorService.lookup(webhook.getName()));
            final ArrayList<ContentPath> inFlight = new ArrayList<>(new TreeSet<>(webhookContentPathSet.getSet(webhook.getName(), WebhookStrategy.createContentPath(webhook))));
            statusBuilder.inFlight(inFlight);
        } catch (Exception e) {
            log.warn("unable to get status " + webhook.getName(), e);
            statusBuilder.errors(Collections.emptyList());
            statusBuilder.inFlight(Collections.emptyList());
        }
    }

    private class WebhookIdleService extends AbstractIdleService {

        @Override
        protected void startUp() {
            start();
        }

        @Override
        protected void shutDown() {
            localWebhookManager.stopAllLocal();
            notifyWatchers();
        }

    }

    private class WebhookScheduledService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
            manageWebhooks(false);
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.MINUTES);
        }
    }
}
