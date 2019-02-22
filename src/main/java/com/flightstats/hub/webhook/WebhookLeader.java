package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
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
import java.util.concurrent.*;
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
    private MetricsService metricsService;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private WebhookContentPathSet webhookInProcess;
    @Inject
    private WebhookErrorService webhookErrorService;
    @Inject
    private WebhookStateReaper webhookStateReaper;

    private Webhook webhook;

    private ExecutorService executorService;
    private Semaphore semaphore;
    private Leadership leadership;
    private WebhookRetryer retryer;

    private WebhookStrategy webhookStrategy;
    private AtomicReference<ContentPath> lastUpdated = new AtomicReference<>();
    private String channelName;
    private CuratorLock curatorLock;

    void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    boolean tryLeadership(Webhook webhook) {
        log.debug("starting webhook: " + webhook);
        setWebhook(webhook);
        if (webhook.isPaused()) {
            log.info("not starting paused webhook " + webhook);
            return false;
        } else {
            String leaderPath = WEBHOOK_LEADER + "/" + webhook.getName();
            curatorLock = new CuratorLock(curator, zooKeeperState, leaderPath);
            return curatorLock.runWithLock(this, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void takeLeadership(Leadership leadership) {
        this.leadership = leadership;
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
            log.info("saw InterruptedException for " + webhook.getName());
        } catch (Exception e) {
            log.warn("Execption for " + webhook.getName(), e);
        } finally {
            log.info("stopping last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            leadership.setLeadership(false);
            closeStrategy();
            if (deleteOnExit.get()) {
                webhookStateReaper.delete(webhook.getName());
            }
            stopExecutor();
            log.info("stopped last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            webhookStrategy = null;
            executorService = null;
        }
    }

    private boolean doesNotHaveLeadership(DeliveryAttempt attempt) {
        if (!leadership.hasLeadership()) {
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
        ChannelConfig channelConfig = channelService.getCachedChannelConfig(webhook.getChannelName());
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
                metricsService.time("webhook.delta", contentPath.getTime().getMillis(), "name:" + webhook.getName());
                long start = System.currentTimeMillis();
                boolean shouldGoToNextItem = retryer.send(webhook, contentPath, webhookStrategy.createResponse(contentPath));
                metricsService.time("webhook", start, "name:" + webhook.getName());
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
        String name = webhook.getName();
        log.info("exiting webhook " + name + "deleting " + delete);
        deleteOnExit.set(delete);
        if (null != curatorLock) {
            curatorLock.stopWorking();
        }
        closeStrategy();
        stopExecutor();
        if (null != curatorLock) {
            curatorLock.delete();
        }
        log.info("exited webhook " + name);
    }

    private void stopExecutor() {
        if (executorService == null) {
            return;
        }
        String name = webhook.getName();
        log.info("stopExecutor " + name);
        try {
            executorService.shutdown();
            log.info("awating termination " + name);
            executorService.awaitTermination(webhook.getCallbackTimeoutSeconds() + 10, TimeUnit.SECONDS);
            log.info("stopped Executor " + name);
        } catch (InterruptedException e) {
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

    boolean hasLeadership() {
        return leadership.hasLeadership();
    }
}
