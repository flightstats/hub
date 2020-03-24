package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.webhook.strategy.WebhookStrategy;
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
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.register;
import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;

@Slf4j
@Singleton
public class WebhookCoordinator {
    private static final String WATCHER_PATH = "/groupCallback/watcher";

    private final LocalWebhookRunner localWebhookRunner;
    private final WebhookErrorService webhookErrorService;
    private final WebhookContentInFlight contentKeysInFlight;
    private final InternalWebhookClient webhookClient;
    private final WebhookStateReaper webhookStateReaper;
    private final ClusterCacheDao clusterCacheDao;
    private final ActiveWebhooks activeWebhooks;
    private final WatchManager watchManager;
    private final Dao<Webhook> webhookDao;

    @Inject
    public WebhookCoordinator(LocalWebhookRunner localWebhookRunner,
                              WebhookErrorService webhookErrorService,
                              WebhookContentInFlight contentKeysInFlight,
                              InternalWebhookClient webhookClient,
                              WebhookStateReaper webhookStateReaper,
                              ClusterCacheDao clusterCacheDao,
                              ActiveWebhooks activeWebhooks,
                              WebhookProperties webhookProperties,
                              WatchManager watchManager,
                              @Named("Webhook") Dao<Webhook> webhookDao) {
        this.localWebhookRunner = localWebhookRunner;
        this.webhookErrorService = webhookErrorService;
        this.contentKeysInFlight = contentKeysInFlight;
        this.webhookClient = webhookClient;
        this.webhookStateReaper = webhookStateReaper;
        this.clusterCacheDao = clusterCacheDao;
        this.activeWebhooks = activeWebhooks;
        this.watchManager = watchManager;
        this.webhookDao = webhookDao;

        if (webhookProperties.isWebhookLeadershipEnabled()) {
            register(new WebhookCoordinatorIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
            register(new WebhookCoordinatorScheduledService(), HubServices.TYPE.AFTER_HEALTHY_START);
        }
    }

    private void start() {
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                manageWebhookLeaders(true);
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        manageWebhookLeaders(false);
    }

    private synchronized void manageWebhookLeaders(boolean useCache) {
        webhookDao.getAll(useCache)
                .forEach(webhook -> ensureRunningOnOnlyOneServer(webhook, false));
    }

    void notifyWatchers(Webhook webhook) {
        ensureRunningOnOnlyOneServer(webhook, true);
    }

    @VisibleForTesting
    void ensureRunningOnOnlyOneServer(Webhook daoWebhook, boolean webhookChanged) {
        if (daoWebhook.getTagUrl() != null && !daoWebhook.getTagUrl().isEmpty()) {
            // tag webhooks are not processed like normal webhooks.
            // they are used as prototype definitions for new webhooks added
            // automatically when a new/existing channel is assigned a tag that is
            // associated with a tag webhook
            return;
        }

        String name = daoWebhook.getName();
        ActiveWebhooks.WebhookState state = activeWebhooks.getState(name);
        WebhookActionDirector director = new WebhookActionDirector(daoWebhook, state, webhookChanged);

        if (director.webhookRequiresNoChanges()) {
            log.debug("no changes required for {}", name);
        } else if (director.webhookShouldStop()) {
            log.debug("stopping {}", name);
            webhookClient.stop(name, state.getRunningServers());
        } else if (director.webhookShouldStart()) {
            log.debug("found v2 webhook {}", name);
            webhookClient.runOnServerWithFewestWebhooks(name);
        } else if (director.webhookShouldRestartOnOneServerAndStopAnyOthers()) {
            log.debug("found existing webhook {} running on {}", name, state.getRunningServers());
            webhookClient.runOnOnlyOneServer(name, state.getRunningServers());
        } else {
            log.warn("the webhook coordinator seems to have a bug; this statement shouldn't be reached");
        }

    }

    @VisibleForTesting
    static class WebhookActionDirector {
        private ActiveWebhooks.WebhookState state;
        private final boolean hasChanged;
        private final Webhook webhook;

        WebhookActionDirector(Webhook webhook, ActiveWebhooks.WebhookState state, boolean hasChanged) {
            this.webhook = webhook;
            this.state = state;
            this.hasChanged = hasChanged;
        }

        boolean webhookShouldStop() {
            return webhook.isPaused() && !state.isStopped();
        }

        boolean webhookShouldStart() {
            return !webhook.isPaused() && state.isStopped();
        }

        boolean webhookRequiresNoChanges() {
            boolean isUnchangedAndRunning = !webhook.isPaused() && state.isRunningOnSingleServer() && !hasChanged;
            boolean isStoppedAndPaused = webhook.isPaused() && state.isStopped();
            return isUnchangedAndRunning || isStoppedAndPaused;
        }

        boolean webhookShouldRestartOnOneServerAndStopAnyOthers() {
            return !webhook.isPaused() && (hasChanged || state.isRunningInAbnormalState());
        }
    }

    private void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void stopLeader(String name) {
        ActiveWebhooks.WebhookState state = activeWebhooks.getState(name);
        webhookClient.stop(name, state.getRunningServers());
        webhookStateReaper.stop(name);
    }

    public void getStatus(Webhook webhook, WebhookStatus.WebhookStatusBuilder statusBuilder) {
        statusBuilder.lastCompleted(clusterCacheDao.get(webhook.getName(), WebhookStrategy.createContentPath(webhook), WEBHOOK_LAST_COMPLETED));
        try {
            statusBuilder.errors(webhookErrorService.lookup(webhook.getName()));
            ArrayList<ContentPath> contentInFlight = new ArrayList<>(new TreeSet<>(contentKeysInFlight.getSet(webhook.getName(), WebhookStrategy.createContentPath(webhook))));
            statusBuilder.inFlight(contentInFlight);
        } catch (Exception e) {
            log.warn("unable to get status {}", webhook.getName(), e);
            statusBuilder.errors(Collections.emptyList());
            statusBuilder.inFlight(Collections.emptyList());
        }
    }

    private class WebhookCoordinatorIdleService extends AbstractIdleService {

        @Override
        protected void startUp() {
            start();
        }

        @Override
        protected void shutDown() {
            localWebhookRunner.stopAll();
            notifyWatchers();
        }

    }

    private class WebhookCoordinatorScheduledService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
            manageWebhookLeaders(false);
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.MINUTES);
        }
    }
}
