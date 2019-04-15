package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.flightstats.hub.webhook.WebhookLeaderLocks.WEBHOOK_LEADER;

@Slf4j
class WebhookLeader implements Lockable {
    static final String WEBHOOK_LAST_COMPLETED = "/GroupLastCompleted/";
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    @Inject
    private CuratorFramework curator;
    @Inject
    private ZooKeeperState zooKeeperState;
    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;
    @Inject
    private StatsdReporter statsdReporter;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private WebhookContentPathSet webhookInProcess;
    @Inject
    private WebhookErrorService webhookErrorService;
    @Inject
    private WebhookStateReaper webhookStateReaper;
    @Inject
    private DistributedLeaderLockManager lockManager;

    private Webhook webhook;

    private ExecutorService executorService;
    private Semaphore semaphore;
    private WebhookRetryer retryer;

    private WebhookStrategy webhookStrategy;
    private AtomicReference<ContentPath> lastUpdated = new AtomicReference<>();
    private String channelName;

    private Optional<LeadershipLock> leadershipLock;
    private DistributedAsyncLockRunner distributedLockRunner;

    boolean tryLeadership(Webhook webhook) {
        log.debug("starting webhook: " + webhook);
        setWebhook(webhook);
        if (webhook.isPaused()) {
            log.info("not starting paused webhook " + webhook);
            leadershipLock = Optional.empty();
        } else {
            String leaderPath = WEBHOOK_LEADER + "/" + webhook.getName();
            distributedLockRunner = new DistributedAsyncLockRunner(leaderPath, lockManager);
            leadershipLock = distributedLockRunner.runWithLock(this, 1, TimeUnit.SECONDS);
        }
        return leadershipLock.isPresent();
    }

    @Override
    public void takeLeadership(Leadership leadership) {
        DLog.log("takeLeadership for " + webhook.getName());
        Optional<Webhook> foundWebhook = webhookService.get(webhook.getName());
        channelName = webhook.getChannelName();
        if (!foundWebhook.isPresent() || !channelService.channelExists(channelName)) {
            log.info("webhook or channel is missing, exiting " + webhook.getName());
            Sleeper.sleep(60 * 1000);
            return;
        }
        this.webhook = foundWebhook.get();
        if (webhook.isPaused()) {
            log.info("webhook is paused " + webhook.getName());
            return;
        }
        log.info("taking leadership {} {}", webhook, leadership.hasLeadership());
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
                .build();
        webhookStrategy = WebhookStrategy.getStrategy(webhook, lastContentPath, channelService);
        try {
            ContentPath lastCompletedPath = webhookStrategy.getStartingPath();
            lastUpdated.set(lastCompletedPath);
            log.info("last completed at {} {}", lastCompletedPath, webhook.getName());
            if (leadership.hasLeadership()) {
                DLog.log("Sending in process " + lastCompletedPath);
                sendInProcess(lastCompletedPath);
                webhookStrategy.start(webhook, lastCompletedPath);
                while (leadership.hasLeadership()) {
                    Optional<ContentPath> nextOptional = webhookStrategy.next();
                    if (nextOptional.isPresent()) {
                        DLog.log("WL send next with leadership " + nextOptional.get().toUrl());
                        send(nextOptional.get());
                    }
                }
            }
        } catch (RuntimeInterruptedException | InterruptedException e) {
            log.info("saw InterruptedException for " + webhook.getName());
        } catch (Exception e) {
            log.warn("Execption for " + webhook.getName(), e);
        } finally {
            DLog.log("WL Stopping");
            log.info("stopping last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            leadership.setLeadership(false);
            closeStrategy();
            stopExecutor();
            if (deleteOnExit.get()) {
                DLog.log("WL takeLeadership(" + webhook.getName() + ") clearing ZK state after abandoning leadership");
                webhookStateReaper.delete(webhook.getName());
            }
            log.info("stopped last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            webhookStrategy = null;
            executorService = null;
            DLog.log("WL Stopped");
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
            DateTime ttlTime = TimeUtil.now().minusMinutes(webhook.getTtlMinutes());
            if (attempt.getContentPath().getTime().isBefore(ttlTime)) {
                String message = String.format("%s is before webhook ttl %s", attempt.getContentPath().toUrl(), ttlTime);
                log.debug(webhook.getName(), message);
                webhookErrorService.add(webhook.getName(), new DateTime() + " " + message);
                return true;
            }
        }
        return false;
    }

    private boolean channelTTLExceeded(DeliveryAttempt attempt) {
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(webhook.getChannelName());
        if (!optionalChannelConfig.isPresent()) return false;
        ChannelConfig channelConfig = optionalChannelConfig.get();
        if (attempt.getContentPath().getTime().isBefore(channelConfig.getTtlTime())) {
            String message = String.format("%s is before channel ttl %s", attempt.getContentPath().toUrl(), channelConfig.getTtlTime());
            log.debug(webhook.getName(), message);
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
            log.debug(webhook.getName() + " " + message);
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
        Set<ContentPath> inProcessSet = webhookInProcess.getSet(webhook.getName(), lastCompletedPath);
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
                webhookInProcess.remove(webhook.getName(), toSend);
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
            webhookInProcess.add(webhook.getName(), contentPath);
            try {
                statsdReporter.time("webhook.delta", contentPath.getTime().getMillis(), "name:" + webhook.getName());
                long start = System.currentTimeMillis();
                DLog.log("WL starting retryer for " + contentPath);
                boolean shouldGoToNextItem = retryer.send(webhook, contentPath, webhookStrategy.createResponse(contentPath));
                DLog.log("WL finished retryer with gotonext " + shouldGoToNextItem + " for " + contentPath);
                statsdReporter.time("webhook", start, "name:" + webhook.getName());
                if (shouldGoToNextItem) {
                    if (increaseLastUpdated(contentPath)) {
                        if (!deleteOnExit.get()) {
                            lastContentPath.updateIncrease(contentPath, webhook.getName(), WEBHOOK_LAST_COMPLETED);
                        }
                    }
                }
                webhookInProcess.remove(webhook.getName(), contentPath);
                log.trace("done sending {} to {} ", contentPath, webhook.getName());
            } catch (Exception e) {
                log.warn("exception sending " + contentPath + " to " + webhook.getName(), e);
            } finally {
                semaphore.release();
                ActiveTraces.end();
                Thread.currentThread().setName(workerName);
            }
            return null;
        });
    }

    private boolean increaseLastUpdated(ContentPath newPath) {
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

    void exit(boolean delete) {
        DLog.log("starting WL.exit(" + delete + ")");
        String name = webhook.getName();
        log.info("exiting webhook " + name + "deleting " + delete);
        deleteOnExit.set(delete);
        closeStrategy();
        stopExecutor();
        leadershipLock.ifPresent(distributedLockRunner::delete);
        log.info("exited webhook " + name);
        DLog.log("finished WL.exit(" + delete + ")");
    }

    private void stopExecutor() {
        DLog.log("WL stopping executor");
        if (executorService == null) {
            return;
        }
        String name = webhook.getName();
        log.info("stopExecutor " + name);
        try {
            executorService.shutdown();
            log.info("awating termination " + name);
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                DLog.log("WL interrupted executor");
                executorService.shutdownNow();
                executorService.awaitTermination(webhook.getCallbackTimeoutSeconds() + 8, TimeUnit.SECONDS);
            } else {
                DLog.log("WL didn't have to interrupt executor");
            }
            log.info("stopped Executor " + name);
        } catch (InterruptedException e) {
            DLog.log("WL unable to stop executor?");
            log.warn("unable to stop?" + name, e);
        }
    }

    private void closeStrategy() {
        try {
            if (webhookStrategy != null) {
                webhookStrategy.close();
            }
        } catch (Exception e) {
            log.warn("unable to close strategy", e);
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
}
