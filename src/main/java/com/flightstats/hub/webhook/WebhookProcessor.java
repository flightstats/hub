package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.register;

public class WebhookProcessor {

    private final static Logger logger = LoggerFactory.getLogger(WebhookProcessor.class);

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    private final WatchManager watchManager;
    private final Dao<Webhook> webhookDao;
    private final Provider<WebhookLeader> leaderProvider;
    private LastContentPath lastContentPath;
    private final Map<String, WebhookLeader> activeWebhooks = new HashMap<>();

    @Inject
    public WebhookProcessor(WatchManager watchManager, @Named("Webhook") Dao<Webhook> webhookDao,
                            Provider<WebhookLeader> leaderProvider, LastContentPath lastContentPath) {
        this.watchManager = watchManager;
        this.webhookDao = webhookDao;
        this.leaderProvider = leaderProvider;
        this.lastContentPath = lastContentPath;
        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
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
        Set<String> webhooksToStop = new HashSet<>(activeWebhooks.keySet());
        Iterable<Webhook> webhooks = webhookDao.getAll(false);
        for (Webhook webhook : webhooks) {
            webhooksToStop.remove(webhook.getName());
            WebhookLeader activeLeader = activeWebhooks.get(webhook.getName());
            if (activeLeader == null) {
                start(webhook);
            } else if (activeLeader.getWebhook().isChanged(webhook)) {
                logger.info("changed webhook {}", webhook);
                activeWebhooks.remove(webhook.getName());
                activeLeader.exit(false);
                start(webhook);
            } else {
                logger.debug("webhook not changed {}", webhook);
            }
        }
        stop(webhooksToStop, true);
    }

    private void stop(Set<String> webhooksToStop, final boolean delete) {
        List<Callable<Object>> callables = new ArrayList<>();
        logger.info("stopping webhooks {}", webhooksToStop);
        for (String webhook : webhooksToStop) {
            logger.info("stopping " + webhook);
            final WebhookLeader webhookLeader = activeWebhooks.remove(webhook);
            callables.add(() -> {
                webhookLeader.exit(delete);
                return null;
            });
        }
        try {
            List<Future<Object>> futures = Executors.newCachedThreadPool().invokeAll(callables, 90, TimeUnit.SECONDS);
            logger.info("stopped webhook " + futures);
        } catch (InterruptedException e) {
            logger.warn("interrupted! ", e);
            throw new RuntimeInterruptedException(e);
        }
    }

    private void start(Webhook webhook) {
        logger.trace("starting webhook {}", webhook);
        WebhookLeader webhookLeader = leaderProvider.get();
        webhookLeader.tryLeadership(webhook);
        activeWebhooks.put(webhook.getName(), webhookLeader);
    }

    public void delete(String name) {
        WebhookLeader webhookLeader = activeWebhooks.get(name);
        if (webhookLeader == null) {
            webhookLeader = leaderProvider.get();
            webhookLeader.setWebhook(Webhook.builder().name(name).build());
        }
        notifyWatchers();
        if (webhookLeader != null) {
            logger.info("deleting...{}", webhookLeader);
            for (int i = 0; i < 30; i++) {
                if (webhookLeader.deleteIfReady()) {
                    logger.info("deleted successfully! " + name);
                    return;
                } else {
                    Sleeper.sleep(1000);
                    logger.info("waiting to delete " + name);
                }
            }
            webhookLeader.deleteAnyway();
        }
    }

    void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void getStatus(Webhook webhook, WebhookStatus.WebhookStatusBuilder statusBuilder) {
        statusBuilder.lastCompleted(lastContentPath.get(webhook.getName(), WebhookStrategy.createContentPath(webhook), WebhookLeader.WEBHOOK_LAST_COMPLETED));
        WebhookLeader webhookLeader = activeWebhooks.get(webhook.getName());
        if (webhookLeader != null) {
            statusBuilder.errors(webhookLeader.getErrors());
            statusBuilder.inFlight(webhookLeader.getInFlight(webhook));
        } else {
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
            stop(new HashSet<>(activeWebhooks.keySet()), false);
        }

    }
}
