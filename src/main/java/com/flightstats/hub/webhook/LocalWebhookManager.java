package com.flightstats.hub.webhook;

import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
@Slf4j
public class LocalWebhookManager {

    private final Map<String, WebhookLeader> localLeaders = new ConcurrentHashMap<>();

    private final Dao<Webhook> webhookDao;
    private final Provider<WebhookLeader> v2Provider;
    private final KeyLockManager lockManager;
    private final int shutdownThreadCount;

    @Inject
    public LocalWebhookManager(@Named("Webhook") Dao<Webhook> webhookDao,
                               Provider<WebhookLeader> v2Provider,
                               WebhookProperties webhookProperties) {
        this.webhookDao = webhookDao;
        this.v2Provider = v2Provider;
        this.lockManager = KeyLockManagers.newLock(1, TimeUnit.SECONDS);
        this.shutdownThreadCount = webhookProperties.getShutdownThreadCount();
    }

    public void runAndWait(String name, Collection<String> keys, Consumer<String> consumer) {
        final ExecutorService pool = Executors.newFixedThreadPool(shutdownThreadCount,
                new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
        log.info("{}", keys);
        for (String key : keys) {
            pool.submit(() -> {
                consumer.accept(key);
            });
        }
        log.info("accepted all ");
        pool.shutdown();
        try {
            boolean awaitTermination = pool.awaitTermination(5, TimeUnit.MINUTES);
            log.info("awaitTermination", awaitTermination);
        } catch (InterruptedException e) {
            log.warn("interuppted", e);
            throw new RuntimeInterruptedException(e);
        }
    }

    boolean ensureRunning(String name) {
        return lockManager.executeLocked(name, () -> ensureRunningWithLock(name));
    }

    private boolean ensureRunningWithLock(String name) {
        Webhook daoWebhook = webhookDao.get(name);
        log.info("ensureRunning {}", daoWebhook);
        if (localLeaders.containsKey(name)) {
            log.info("checking for change {}", name);
            WebhookLeader webhookLeader = localLeaders.get(name);
            Webhook runningWebhook = webhookLeader.getWebhook();
            if (webhookLeader.hasLeadership() && !runningWebhook.isChanged(daoWebhook)) {
                log.trace("webhook unchanged {} to {}", runningWebhook, daoWebhook);
                return true;
            }
            log.info("webhook has changed {} to {}", runningWebhook, daoWebhook);
            stopLocal(name, false);
        }
        log.info("starting {}", name);
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

    void stopAllLocal() {
        runAndWait("LocalWebhookManager.stopAll", localLeaders.keySet(), (name) -> stopLocal(name, false));
    }

    void stopLocal(String name, boolean delete) {
        log.info("stop {} {}", name, delete);
        if (localLeaders.containsKey(name)) {
            log.info("stopping local {}", name);
            localLeaders.get(name).exit(delete);
            localLeaders.remove(name);
        }
    }

    int getCount() {
        return localLeaders.size();
    }
}