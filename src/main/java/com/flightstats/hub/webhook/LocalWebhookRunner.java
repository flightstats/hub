package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.StatsdReporter;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final StatsdReporter statsdReporter;
    private final KeyLockManager lockManager;
    private final int shutdownThreadCount;
    private final LocalLeaders localLeaders;

    @Inject
    public LocalWebhookRunner(@Named("Webhook") Dao<Webhook> webhookDao,
                              Provider<WebhookLeader> webhookLeaderProvider,
                              WebhookProperties webhookProperties,
                              StatsdReporter statsdReporter) {
        this.webhookDao = webhookDao;
        this.webhookLeaderProvider = webhookLeaderProvider;
        this.statsdReporter = statsdReporter;
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

        return localLeaders.getLocalLeader(name).map(existingLeader -> {
            log.debug("checking for change {}", name);
            Webhook runningWebhook = existingLeader.getWebhook();
            if (existingLeader.hasLeadership() && !runningWebhook.isChanged(daoWebhook)) {
                log.trace("webhook unchanged {} to {}", runningWebhook, daoWebhook);
                return true;
            }
            log.info("webhook has changed {} to {}; stopping {}", runningWebhook, daoWebhook, runningWebhook);
            stopWithLock(existingLeader);

            log.info("restarting {}", name);
            return start(daoWebhook);
        }).orElseGet(() -> {
            log.info("starting {}", name);
            return start(daoWebhook);
        });
    }

    private boolean start(Webhook daoWebhook) {
        WebhookLeader webhookLeader = webhookLeaderProvider.get();
        return webhookLeader.tryLeadership(daoWebhook, localLeaders);
    }

    void stopAll() {
        List<String> webhookNames = localLeaders.getLocalLeaders().stream()
                .map(v -> v.getWebhook().getName())
                .collect(Collectors.toList());
        runAndWait("LocalWebhookRunner.stopAll", webhookNames, this::stop);
    }

    void stop(String name) {
        lockManager.executeLocked(name, () -> stopWithLock(name));
    }

    private void stopWithLock(String name) {
        log.info("stopping {} if running local", name);
        localLeaders.getLocalLeader(name).ifPresent(this::stopWithLock);
    }

    private void stopWithLock(WebhookLeader webhookLeader) {
        log.info("stopping local {}", webhookLeader);
        webhookLeader.exit();
    }

    int getCount() {
        return localLeaders.getLocalLeaders().size();
    }

    Map<String, Map<String, Object>> getRunning() {
        return localLeaders.getLocalLeaders().stream()
                .collect(Collectors.toMap(k -> k.getWebhook().getName(), WebhookLeader::getLocalStatistics));
    }


    class LocalLeaders implements WebhookLeader.LeadershipStateListener {
        private final Map<String, WebhookLeader> localLeaders = new ConcurrentHashMap<>();

        public Optional<WebhookLeader> getLocalLeader(String webhookName) {
            return Optional.ofNullable(localLeaders.get(webhookName));
        }

        public Collection<WebhookLeader> getLocalLeaders() {
            return localLeaders.values();
        }

        @Override
        public void leadershipStateUpdated(WebhookLeader webhookLeader, boolean hasLeadership) {
            String webhookName = webhookLeader.getWebhook().getName();

            if (hasLeadership) {
                WebhookLeader existingLeader = localLeaders.put(webhookName, webhookLeader);
                if (null != existingLeader) {
                    log.error("Starting a second webhook leader for {}", existingLeader);
                    statsdReporter.incrementCounter("webhook.leader.over", "name:" + webhookName);
                }
            }
            else {
                if(!localLeaders.remove(webhookName, webhookLeader)) {
                    log.error("Attempted to remove an unexpected webhook leader for {}", webhookName);
                    statsdReporter.incrementCounter("webhook.leader.under", "name:" + webhookName);
                }
            }
        }
    }
}