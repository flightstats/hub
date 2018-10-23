package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class LocalWebhookManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalWebhookManager.class);

    private final Map<String, WebhookLeader> localLeaders = new ConcurrentHashMap<>();
    private final KeyLockManager lockManager = KeyLockManagers.newLock(1, TimeUnit.SECONDS);
    private final Dao<Webhook> webhookDao;
    private final Provider<WebhookLeader> v2Provider;
    private final HubProperties hubProperties;

    @Inject
    public LocalWebhookManager(@Named("Webhook") Dao<Webhook> webhookDao,
                               Provider<WebhookLeader> v2Provider,
                               HubProperties hubProperties)
    {
        this.webhookDao = webhookDao;
        this.v2Provider = v2Provider;
        this.hubProperties = hubProperties;
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
            if (webhookLeader.hasLeadership() && !runningWebhook.isChanged(daoWebhook)) {
                logger.trace("webhook unchanged {} to {}", runningWebhook, daoWebhook);
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

    void stopAllLocal() throws InterruptedException {
        runAndWait("LocalWebhookManager.stopAll", localLeaders.keySet(), (name) -> stopLocal(name, false));
    }

    void runAndWait(String name, Collection<String> keys, Consumer<String> consumer) {
        int threadCount = hubProperties.getProperty("webhook.shutdown.threads", 100);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount, new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
        logger.info("{}", keys);
        for (String key : keys) {
            pool.submit(() -> {
                consumer.accept(key);
            });
        }
        logger.info("accepted all ");
        pool.shutdown();
        try {
            boolean awaitTermination = pool.awaitTermination(5, TimeUnit.MINUTES);
            logger.info("awaitTermination", awaitTermination);
        } catch (InterruptedException e) {
            logger.warn("interuppted", e);
            throw new RuntimeInterruptedException(e);
        }
    }

    void stopLocal(String name, boolean delete) {
        logger.info("stop {} {}", name, delete);
        if (localLeaders.containsKey(name)) {
            logger.info("stopping local {}", name);
            localLeaders.get(name).exit(delete);
            localLeaders.remove(name);
        }
    }

    int getCount() {
        return localLeaders.size();
    }
}
