package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.cluster.DistributedLeaderLockManager;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.LeadershipLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsType;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.strategy.WebhookStrategy;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;
import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LEADER;

@Slf4j
class WebhookLeader {
    private static final MetricsType LEADERSHIP_METRIC = MetricsType.WEBHOOK_LEADERSHIP;

    private final AtomicReference<ContentPath> lastUpdated = new AtomicReference<>();

    private final ContentRetriever contentRetriever;
    private final WebhookService webhookService;
    private final StatsdReporter statsdReporter;
    private final ClusterCacheDao clusterCacheDao;
    private final WebhookContentInFlight keysInFlight;
    private final WebhookErrorService webhookErrorService;
    private final WebhookStateReaper webhookStateReaper;
    private final DistributedLeaderLockManager lockManager;
    private final WebhookProperties webhookProperties;
    private final LocalHostProperties localHostProperties;
    private final ObjectMapper objectMapper;

    private DistributedAsyncLockRunner distributedLockRunner;
    private Optional<LeadershipLock> leadershipLock;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private WebhookRetryer retryer;
    private WebhookStrategy webhookStrategy;
    private Webhook webhook;
    private Long leadershipStartTime;


    @Inject
    public WebhookLeader(ContentRetriever contentRetriever,
                         WebhookService webhookService,
                         StatsdReporter statsdReporter,
                         ClusterCacheDao clusterCacheDao,
                         WebhookContentInFlight keysInFlight,
                         WebhookErrorService webhookErrorService,
                         WebhookStateReaper webhookStateReaper,
                         DistributedLeaderLockManager lockManager,
                         WebhookProperties webhookProperties,
                         LocalHostProperties localHostProperties,
                         ObjectMapper objectMapper) {
        this.contentRetriever = contentRetriever;
        this.webhookService = webhookService;
        this.statsdReporter = statsdReporter;
        this.clusterCacheDao = clusterCacheDao;
        this.keysInFlight = keysInFlight;
        this.webhookErrorService = webhookErrorService;
        this.webhookStateReaper = webhookStateReaper;
        this.lockManager = lockManager;
        this.webhookProperties = webhookProperties;
        this.localHostProperties = localHostProperties;
        this.objectMapper = objectMapper;
    }

    boolean tryLeadership(Webhook webhook, LeadershipStateListener leadershipStateListener) {
        log.debug("starting webhook: {}", webhook);
        setWebhook(webhook);
        if (webhook.isPaused()) {
            log.debug("not starting paused webhook {}", webhook);
            leadershipLock = Optional.empty();
        } else {
            String leaderPath = WEBHOOK_LEADER + "/" + webhook.getName();
            distributedLockRunner = new DistributedAsyncLockRunner(leaderPath, lockManager);
            leadershipLock = distributedLockRunner.runWithLock(() -> {
                leadershipStateListener.leadershipStateUpdated(this, true);
                runWebhookLeader()
                leadershipStateListener.leadershipStateUpdated(this, false);
            }, 1, TimeUnit.SECONDS);
        }
        return leadershipLock.isPresent();
    }

    private void runWebhookLeader(Leadership leadership) {
        leadershipStartTime = System.currentTimeMillis();
        Optional<Webhook> foundWebhook = webhookService.get(webhook.getName());
        String channelName = webhook.getChannelName();
        if (!foundWebhook.isPresent() || !this.contentRetriever.isExistingChannel(channelName)) {
            log.warn("webhook or channel is missing, exiting {}", webhook.getName());
            Sleeper.sleep(60 * 1000);
            return;
        }
        this.webhook = foundWebhook.get();
        if (webhook.isPaused()) {
            log.info("webhook {} is paused", webhook.getName());
            return;
        }
        log.info("taking leadership {} {}", webhook.getName(), leadership.hasLeadership());
        statsdReporter.incrementEventStart(LEADERSHIP_METRIC, "name:" + webhook.getName());
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(webhook.getParallelCalls());
        retryer = WebhookRetryer.builder()
                .readTimeoutSeconds(webhook.getCallbackTimeoutSeconds())
                .tryLaterIf(this::doesNotHaveLeadership)
                .tryLaterIf(this::webhookIsPaused)
                .tryLaterIf(this::retryerInterrupted)
                .giveUpIf(this::webhookTTLExceeded)
                .giveUpIf(this::channelTTLExceeded)
                .giveUpIf(this::maxAttemptsReached)
                .webhookErrorService(webhookErrorService)
                .statsdReporter(statsdReporter)
                .webhookProperties(webhookProperties)
                .localHostProperties(localHostProperties)
                .build();
        webhookStrategy = WebhookStrategy.getStrategy(contentRetriever, clusterCacheDao, objectMapper, webhook);
        try {
            final ContentPath lastCompletedPath = webhookStrategy.getStartingPath();
            lastUpdated.set(lastCompletedPath);
            log.debug("last completed at {} {}", lastCompletedPath, webhook.getName());
            if (leadership.hasLeadership()) {
                sendInProcess(lastCompletedPath);
                webhookStrategy.start(webhook, lastCompletedPath);
                while (leadership.hasLeadership()) {
                    Optional<ContentPath> nextOptional = webhookStrategy.next();
                    if (nextOptional.isPresent()) {
                        send(nextOptional.get());
                    }
                }
            }
        } catch (RuntimeInterruptedException | InterruptedException e) {
            log.warn("saw InterruptedException for {}", webhook.getName());
        } catch (Exception e) {
            log.error("Execption for " + webhook.getName(), e);
        } finally {
            log.debug("stopping last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            leadership.setLeadership(false);
            closeStrategy();
            stopExecutor();
            webhookStateReaper.stop(webhook.getName());
            log.info("stopped last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            statsdReporter.incrementEventCompletion(LEADERSHIP_METRIC, "name:" + webhook.getName());
            webhookStrategy = null;
            executorService = null;
        }
    }

    private boolean doesNotHaveLeadership(DeliveryAttempt attempt) {
        if (!hasLeadership()) {
            log.debug("{} {} not the leader", webhook.getName(), attempt.getContentPath());
            return true;
        } else {
            return false;
        }
    }

    private boolean webhookTTLExceeded(DeliveryAttempt attempt) {
        if (webhook.getTtlMinutes() > 0) {
            final DateTime ttlTime = TimeUtil.now().minusMinutes(webhook.getTtlMinutes());
            if (attempt.getContentPath().getTime().isBefore(ttlTime)) {
                final String message = String.format("%s is before webhook ttl %s", attempt.getContentPath().toUrl(), ttlTime);
                log.debug("{}: {}", webhook.getName(), message);
                webhookErrorService.add(webhook.getName(), new DateTime() + " " + message);
                return true;
            }
        }
        return false;
    }

    private boolean channelTTLExceeded(DeliveryAttempt attempt) {
        final Optional<ChannelConfig> optionalChannelConfig =
                this.contentRetriever.getCachedChannelConfig(webhook.getChannelName());
        if (!optionalChannelConfig.isPresent()) return false;
        ChannelConfig channelConfig = optionalChannelConfig.get();
        if (attempt.getContentPath().getTime().isBefore(channelConfig.getTtlTime())) {
            String message = String.format("%s is before channel ttl %s", attempt.getContentPath().toUrl(), channelConfig.getTtlTime());
            log.debug("{}: {}", webhook.getName(), message);
            webhookErrorService.add(webhook.getName(), new DateTime() + " " + message);
            return true;
        } else {
            return false;
        }
    }

    private boolean maxAttemptsReached(DeliveryAttempt attempt) {
        int maxAttempts = attempt.getWebhook().getMaxAttempts();
        if (maxAttempts > 0 && attempt.number > maxAttempts) {
            String message = String.format("%s max attempts reached (%s)", attempt.getContentPath().toUrl(), maxAttempts);
            log.debug("{}: {}", webhook.getName(), message);
            webhookErrorService.add(webhook.getName(), new DateTime() + " " + message);
            return true;
        } else {
            return false;
        }
    }

    private boolean webhookIsPaused(DeliveryAttempt attempt) {
        if (webhook.isPaused()) {
            log.debug("{} {} webhook paused", webhook.getName(), attempt.getContentPath().toUrl());
            return true;
        } else {
            return false;
        }
    }

    private boolean retryerInterrupted(DeliveryAttempt attempt) {
        return Thread.currentThread().isInterrupted();
    }

    private void sendInProcess(ContentPath lastCompletedPath) throws InterruptedException {
        Set<ContentPath> inProcessSet = keysInFlight.getSet(webhook.getName(), lastCompletedPath);
        log.debug("sending in process {} to {}", inProcessSet, webhook.getName());
        for (ContentPath toSend : inProcessSet) {
            if (toSend.compareTo(lastCompletedPath) < 0) {
                ActiveTraces.start("WebhookLeader inProcess", webhook);
                ContentPath contentPath;
                try {
                    contentPath = webhookStrategy.inProcess(toSend);
                } finally {
                    ActiveTraces.end();
                }
                send(contentPath);
            } else {
                keysInFlight.remove(webhook.getName(), toSend);
            }
        }
    }

    private void send(ContentPath contentPath) throws InterruptedException {
        semaphore.acquire();
        log.trace("sending {} to {}", contentPath, webhook.getName());
        String parentName = Thread.currentThread().getName();
        executorService.submit(() -> {
            String workerName = Thread.currentThread().getName();
            Thread.currentThread().setName(workerName + "|" + parentName);
            ActiveTraces.start("WebhookLeader.send", webhook, contentPath);
            keysInFlight.add(webhook.getName(), contentPath);
            try {
                statsdReporter.time("webhook.delta", contentPath.getTime().getMillis(), "name:" + webhook.getName());
                long start = System.currentTimeMillis();
                boolean shouldGoToNextItem = retryer.send(webhook, contentPath, webhookStrategy.createResponse(contentPath));
                statsdReporter.time("webhook", start, "name:" + webhook.getName());
                if (shouldGoToNextItem && increaseLastUpdatedIfNewer(contentPath)) {
                    clusterCacheDao.setIfNewer(contentPath, webhook.getName(), WEBHOOK_LAST_COMPLETED);
                }
                keysInFlight.remove(webhook.getName(), contentPath);
                log.trace("done sending {} to {} ", contentPath, webhook.getName());
            } catch (Exception e) {
                log.warn("exception sending {} to {}", contentPath, webhook.getName(), e);
            } finally {
                semaphore.release();
                ActiveTraces.end();
                Thread.currentThread().setName(workerName);
            }
            return null;
        });
    }

    private boolean increaseLastUpdatedIfNewer(ContentPath newPath) {
        AtomicBoolean changed = new AtomicBoolean(false);
        lastUpdated.getAndUpdate(existingPath -> {
            if (newPath.compareTo(existingPath) > 0) {
                changed.set(true);
                return newPath;
            }
            changed.set(false);
            return existingPath;
        });
        return changed.get();
    }

    void exit() {
        String name = webhook.getName();
        log.debug("stopping leadership on webhook {}", name);
        leadershipLock.ifPresent(distributedLockRunner::delete);
        closeStrategy();
        stopExecutor();
        log.info("exited webhook {}", name);
    }

    private void stopExecutor() {
        if (executorService == null) {
            return;
        }
        String name = webhook.getName();
        log.debug("stopExecutor " + name);
        try {
            log.debug("awating termination {}", name);
            executorService.shutdownNow();
            executorService.awaitTermination(webhook.getCallbackTimeoutSeconds() + 10, TimeUnit.SECONDS);
            log.info("stopped Executor {}", name);
        } catch (InterruptedException e) {
            log.error("unable to stop {}?", name, e);
            Thread.currentThread().interrupt();
        }
    }

    private void closeStrategy() {
        try {
            if (webhookStrategy != null) {
                webhookStrategy.close();
            }
        } catch (Exception e) {
            log.error("unable to close strategy", e);
        }
    }

    public Webhook getWebhook() {
        return webhook;
    }

    void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    boolean hasLeadership() {
        return leadershipLock
                .map(lock -> lock.getLeadership().hasLeadership())
                .orElse(false);
    }

    Map<String,Object> getLocalStatistics() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        return builder
                .put("startTimeMillis", leadershipStartTime)
                .put("lastUpdated", lastUpdated.get())
                .put("hasLeadership", hasLeadership())
                .build();
    }

    interface LeadershipStateListener {
        void leadershipStateUpdated(WebhookLeader webhookLeader, boolean hasLeadership);
    }
}