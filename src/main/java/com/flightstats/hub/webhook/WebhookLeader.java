package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.RecurringTrace;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.github.rholder.retry.RetryException;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class WebhookLeader implements Lockable {

    final static String WEBHOOK_LAST_COMPLETED = "/GroupLastCompleted/";

    private final static Logger logger = LoggerFactory.getLogger(WebhookLeader.class);
    private final static String LEADER_PATH = "/WebhookLeader";

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
    private WebhookError webhookError;

    private Webhook webhook;

    private ExecutorService executorService;
    private Semaphore semaphore;
    private Leadership leadership;
    private WebhookCarrier carrier;

    private WebhookStrategy webhookStrategy;
    private AtomicReference<ContentPath> lastUpdated = new AtomicReference<>();
    private String channelName;
    private CuratorLock curatorLock;

    void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    boolean tryLeadership(Webhook webhook) {
        logger.debug("starting webhook: " + webhook);
        setWebhook(webhook);
        if (webhook.isPaused()) {
            logger.info("not starting paused webhook " + webhook);
            return false;
        } else {
            curatorLock = new CuratorLock(curator, zooKeeperState, getLeaderPath());
            return curatorLock.runWithLock(this, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void takeLeadership(Leadership leadership) {
        this.leadership = leadership;
        Optional<Webhook> foundWebhook = webhookService.get(webhook.getName());
        channelName = webhook.getChannelName();
        if (!foundWebhook.isPresent() || !channelService.channelExists(channelName)) {
            logger.info("webhook or channel is missing, exiting " + webhook.getName());
            Sleeper.sleep(60 * 1000);
            return;
        }
        this.webhook = foundWebhook.get();
        if (webhook.isPaused()) {
            logger.info("webhook is paused " + webhook.getName());
            return;
        }
        logger.info("taking leadership {} {}", webhook, leadership.hasLeadership());
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(webhook.getParallelCalls());
        carrier = WebhookCarrier.builder()
                .timeoutSeconds(webhook.getCallbackTimeoutSeconds())
                .stopBeforeIf(this::doesNotHaveLeadership)
                .stopBeforeIf(this::webhookIsPaused)
                .stopBeforeIf(this::webhookTTLExceeded)
                .stopBeforeIf(this::channelTTLExceeded)
                .stopBeforeIf(this::maxAttemptsReached)
                .stopAfterIf(this::isSuccessfulDelivery)
                .build();
        webhookStrategy = WebhookStrategy.getStrategy(webhook, lastContentPath, channelService);
        try {
            ContentPath lastCompletedPath = webhookStrategy.getStartingPath();
            lastUpdated.set(lastCompletedPath);
            logger.info("last completed at {} {}", lastCompletedPath, webhook.getName());
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
            logger.info("saw InterruptedException for " + webhook.getName());
        } catch (Exception e) {
            logger.warn("Execption for " + webhook.getName(), e);
        } finally {
            logger.info("stopping last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            leadership.setLeadership(false);
            closeStrategy();
            if (deleteOnExit.get()) {
                delete();
            }
            stopExecutor();
            logger.info("stopped last completed at {} {}", webhookStrategy.getLastCompleted(), webhook.getName());
            webhookStrategy = null;
            executorService = null;
        }
    }

    private boolean doesNotHaveLeadership(DeliveryAttempt attempt) {
        if (!leadership.hasLeadership()) {
            logger.debug("{} {} not the leader", webhook.getName(), attempt.getContentPath());
            return true;
        } else {
            return false;
        }
    }

    private boolean webhookTTLExceeded(DeliveryAttempt attempt) {
        if (webhook.getTtlMinutes() > 0) {
            DateTime ttlTime = TimeUtil.now().minusMinutes(webhook.getTtlMinutes());
            if (attempt.getContentPath().getTime().isBefore(ttlTime)) {
                String message = String.format("%s is before %s", attempt.getContentPath().toUrl(), ttlTime);
                logger.debug(webhook.getName(), message);
                webhookError.add(webhook.getName(), message);
                return true;
            }
        }
        return false;
    }

    private boolean channelTTLExceeded(DeliveryAttempt attempt) {
        ChannelConfig channelConfig = channelService.getCachedChannelConfig(webhook.getChannelName());
        if (attempt.getContentPath().getTime().isBefore(channelConfig.getTtlTime())) {
            String message = String.format("%s is before channel ttl %s", attempt.getContentPath().toUrl(), channelConfig.getTtlTime());
            logger.debug(webhook.getName(), message);
            webhookError.add(webhook.getName(), message);
            return true;
        } else {
            return false;
        }
    }

    private boolean maxAttemptsReached(DeliveryAttempt attempt) {
        if (webhook.getMaxAttempts() > 0 && attempt.number >= webhook.getMaxAttempts()) {
            logger.debug("{} {} max attempts reached", webhook.getName(), attempt.getContentPath().toUrl());
            return true;
        } else {
            return false;
        }
    }

    private boolean webhookIsPaused(DeliveryAttempt attempt) {
        if (webhook.isPaused()) {
            logger.debug("{} {} webhook paused", webhook.getName(), attempt.getContentPath().toUrl());
            return true;
        } else {
            return false;
        }
    }

    private boolean isSuccessfulDelivery(DeliveryAttempt attempt) {
        boolean isSuccessResponse = attempt.getResponse().getStatus() >= 200 && attempt.getResponse().getStatus() < 300;
        boolean isRedirectResponse = attempt.getResponse().getStatus() >= 300 && attempt.getResponse().getStatus() < 400;
        if (isSuccessResponse || isRedirectResponse) {
            logger.debug("{} {} successful delivery (http {})", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getResponse().getStatus());
            return true;
        } else {
            return false;
        }
    }

    private void sendInProcess(ContentPath lastCompletedPath) throws InterruptedException {
        Set<ContentPath> inProcessSet = webhookInProcess.getSet(webhook.getName(), lastCompletedPath);
        logger.debug("sending in process {} to {}", inProcessSet, webhook.getName());
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
        logger.trace("sending {} to {}", contentPath, webhook.getName());
        String parentName = Thread.currentThread().getName();
        executorService.submit(() -> {
            String workerName = Thread.currentThread().getName();
            Thread.currentThread().setName(workerName + "|" + parentName);
            ActiveTraces.start("WebhookLeader.send", webhook, contentPath);
            webhookInProcess.add(webhook.getName(), contentPath);
            try {
                metricsService.time("webhook.delta", contentPath.getTime().getMillis(), "name:" + webhook.getName());
                makeTimedCall(contentPath, webhookStrategy.createResponse(contentPath));
                completeCall(contentPath);
                logger.trace("completed {} call to {} ", contentPath, webhook.getName());
            } catch (Exception e) {
                logger.warn("exception sending " + contentPath + " to " + webhook.getName(), e);
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

    private void completeCall(ContentPath contentPath) {
        if (increaseLastUpdated(contentPath)) {
            if (!deleteOnExit.get()) {
                lastContentPath.updateIncrease(contentPath, webhook.getName(), WEBHOOK_LAST_COMPLETED);
            }
        }
        webhookInProcess.remove(webhook.getName(), contentPath);
    }

    private void makeTimedCall(ContentPath contentPath, ObjectNode body) throws Exception {
        metricsService.time("webhook", webhook.getName(), () -> {
            makeCall(contentPath, body);
            return null;
        });
    }

    private void makeCall(ContentPath contentPath, ObjectNode body) throws ExecutionException, RetryException {
        Traces traces = ActiveTraces.getLocal();
        traces.add("WebhookLeader.makeCall start");
        RecurringTrace recurringTrace = new RecurringTrace("WebhookLeader.makeCall start");
        traces.add(recurringTrace);
        carrier.send(webhook, contentPath, body);
        recurringTrace.update("WebhookLeader.makeCall completed");
    }

    void exit(boolean delete) {
        String name = webhook.getName();
        logger.info("exiting webhook " + name + "deleting " + delete);
        deleteOnExit.set(delete);
        if (null != curatorLock) {
            curatorLock.stopWorking();
        }
        closeStrategy();
        stopExecutor();
        if (null != curatorLock) {
            curatorLock.delete();
        }
        logger.info("exited webhook " + name);
    }

    private void stopExecutor() {
        if (executorService == null) {
            return;
        }
        String name = webhook.getName();
        logger.info("stopExecutor " + name);
        try {
            executorService.shutdown();
            logger.info("awating termination " + name);
            executorService.awaitTermination(webhook.getCallbackTimeoutSeconds() + 10, TimeUnit.SECONDS);
            logger.info("stopped Executor " + name);
        } catch (InterruptedException e) {
            logger.warn("unable to stop?" + name, e);
        }
    }

    private void closeStrategy() {
        try {
            if (webhookStrategy != null) {
                webhookStrategy.close();
            }
        } catch (Exception e) {
            logger.warn("unable to close strategy", e);
        }
    }

    private String getLeaderPath() {
        return LEADER_PATH + "/" + webhook.getName();
    }

    private void delete() {
        String name = webhook.getName();
        logger.info("deleting " + name);
        webhookInProcess.delete(name);
        lastContentPath.delete(name, WEBHOOK_LAST_COMPLETED);
        webhookError.delete(name);
        logger.info("deleted " + name);
    }

    public Webhook getWebhook() {
        return webhook;
    }

    boolean hasLeadership() {
        return leadership.hasLeadership();
    }
}
