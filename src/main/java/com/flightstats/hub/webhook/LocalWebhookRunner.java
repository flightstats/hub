package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class LocalWebhookRunner {
    private final Dao<Webhook> webhookDao;
    private final Provider<WebhookLeader> webhookLeaderProvider;
    private final KeyLockManager lockManager;
    private final int shutdownThreadCount;
    private final LocalLeaders localLeaders;

    @Inject
    public LocalWebhookRunner(@Named("Webhook") Dao<Webhook> webhookDao,
                              Provider<WebhookLeader> webhookLeaderProvider,
                              WebhookProperties webhookProperties) {
        this.webhookDao = webhookDao;
        this.webhookLeaderProvider = webhookLeaderProvider;
        this.lockManager = KeyLockManagers.newLock(1, TimeUnit.SECONDS);
        this.shutdownThreadCount = webhookProperties.getShutdownThreadCount();
        this.localLeaders = new LocalLeaders();
    }

    public void runAndWait(String name, Collection<String> keys, Consumer<String> consumer) {
        ExecutorService pool = Executors.newFixedThreadPool(shutdownThreadCount,
                new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
        log.debug("processing {}", keys);
        for (String key : keys) {
            pool.submit(() -> {
                consumer.accept(key);
            });
        }
        log.info("accepted all keys");
        pool.shutdown();
        try {
            boolean awaitTermination = pool.awaitTermination(5, TimeUnit.MINUTES);
            log.debug("awaitTermination {}", awaitTermination);
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
            throw new RuntimeInterruptedException(e);
        }
    }

    /**
     * @return true if the webhook acquired had leadership at any point.
     * This is not a valid indicator of whether or not the webhook still has leadership.
     */
    boolean ensureRunning(String name) {
        return lockManager.executeLocked(name, () -> ensureRunningWithLock(name));
    }

    private boolean ensureRunningWithLock(String name) {
        Webhook daoWebhook = webhookDao.get(name);
        log.debug("ensureRunning {}", daoWebhook);
        WebhookLeader existingLeader = localLeaders.getLocalLeaders().get(name);
        if (null != existingLeader) {
            log.debug("checking for change {}", name);
            Webhook runningWebhook = existingLeader.getWebhook();
            if (existingLeader.hasLeadership() && !runningWebhook.isChanged(daoWebhook)) {
                log.trace("webhook unchanged {} to {}", runningWebhook, daoWebhook);
                return true;
            }
            log.info("webhook has changed {} to {}; stopping", runningWebhook, daoWebhook);
            stopWithLock(name);
        }
        log.info("starting {}", name);
        return start(daoWebhook);
    }

    private boolean start(Webhook daoWebhook) {
        WebhookLeader webhookLeader = webhookLeaderProvider.get();
        return webhookLeader.tryLeadership(daoWebhook, localLeaders);
    }

    void stopAll() {
        runAndWait("LocalWebhookRunner.stopAll", localLeaders.getLocalLeaders().keySet(), this::stop);
    }

    void stop(String name) {
        lockManager.executeLocked(name, () -> stopWithLock(name));
    }

    private void stopWithLock(String name) {
        log.info("stopping {} if running local", name);
        WebhookLeader webhookLeader = localLeaders.getLocalLeaders().get(name);
        if (null != webhookLeader) {
            log.info("stopping local {}", name);
            webhookLeader.exit();
        }
    }

    int getCount() {
        return localLeaders.getLocalLeaders().size();
    }

    Map<String, Map<String, Object>> getRunning() {
        return localLeaders.getLocalLeaders().values().stream()
                .collect(Collectors.toMap(k -> k.getWebhook().getName(), WebhookLeader::getLocalStatistics));
    }


    static class LocalLeaders implements WebhookLeader.LeadershipStateListener {
        private final Map<String, WebhookLeader> localLeaders = new ConcurrentHashMap<>();

        public Map<String, WebhookLeader> getLocalLeaders() {
            return localLeaders;
        }

        @Override
        public void leadershipStateUpdated(WebhookLeader webhookLeader, boolean hasLeadership) {
            String webhookName = webhookLeader.getWebhook().getName();

            if (hasLeadership) {
                WebhookLeader existingLeader = localLeaders.put(webhookName, webhookLeader);
                if (null != existingLeader) {
                    log.error("Starting a second webhook leader for {}", webhookName);
                }
            }
            else {
                if(!localLeaders.remove(webhookName, webhookLeader)) {
                    log.error("Attempted to remove an unexpected webhook leader for {}", webhookName);
                }
            }
        }
    }
}