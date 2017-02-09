package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.RecurringTrace;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.newrelic.api.agent.Trace;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class WebhookLeader implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(WebhookLeader.class);
    static final String WEBHOOK_LAST_COMPLETED = "/GroupLastCompleted/";

    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    @Inject
    private CuratorFramework curator;
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
    private CuratorLeader curatorLeader;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private Leadership leadership;
    private Retryer<ClientResponse> retryer;
    private Client client;

    private WebhookStrategy webhookStrategy;
    private AtomicReference<ContentPath> lastUpdated = new AtomicReference<>();
    private String id = RandomStringUtils.randomAlphanumeric(4);

    void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    boolean tryLeadership(Webhook webhook) {
        logger.debug("starting webhook: " + webhook);
        setWebhook(webhook);
        curatorLeader = new CuratorLeader(getLeaderPath(), this);
        if (!webhook.isPaused()) {
            curatorLeader.start();
        } else {
            logger.info("not starting paused webhook " + webhook);
        }
        return true;
    }

    @Override
    public void takeLeadership(Leadership leadership) {
        this.leadership = leadership;
        Optional<Webhook> foundWebhook = webhookService.getCached(webhook.getName());
        if (!foundWebhook.isPresent() || !channelService.channelExists(webhook.getChannelName())) {
            logger.info("webhook or channel is missing, exiting " + webhook.getName());
            Sleeper.sleep(60 * 1000);
            return;
        }
        this.webhook = foundWebhook.get();
        logger.info("taking leadership {} {}", webhook, leadership.hasLeadership());
        client = RestClient.createClient(60, webhook.getCallbackTimeoutSeconds(), true, false);
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(webhook.getParallelCalls());
        retryer = WebhookRetryer.buildRetryer(webhook, webhookError, leadership);
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
            client = null;
        }
    }

    @Override
    public String getId() {
        return id;
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
        executorService.submit(new Callable<Object>() {
            @Trace(metricName = "WebhookCaller", dispatcher = true)
            @Override
            public Object call() throws Exception {
                String workerName = Thread.currentThread().getName();
                Thread.currentThread().setName(workerName + "|" + parentName);
                ActiveTraces.start("WebhookLeader.send", webhook, contentPath);
                webhookInProcess.add(webhook.getName(), contentPath);
                try {
                    metricsService.time("webhook.delta", contentPath.getTime().getMillis(), "name:" + webhook.getName());
                    makeTimedCall(contentPath, webhookStrategy.createResponse(contentPath));
                    completeCall(contentPath);
                    logger.trace("completed {} call to {} ", contentPath, webhook.getName());
                } catch (RetryException e) {
                    logger.info("exception sending {} to {} {} ", contentPath, webhook.getName(), e.getMessage());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ItemExpiredException) {
                        logger.info("stopped trying {} to {} {} ", contentPath, webhook.getName(), cause.getMessage());
                        completeCall(contentPath);
                    }
                } catch (Exception e) {
                    logger.warn("exception sending " + contentPath + " to " + webhook.getName(), e);
                } finally {
                    semaphore.release();
                    ActiveTraces.end();
                    Thread.currentThread().setName(workerName);
                }
                return null;
            }
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
        metricsService.time("webhook", webhook.getName(),
                () -> {
                    makeCall(contentPath, body);
                    return null;
                });
    }

    private void makeCall(ContentPath contentPath, ObjectNode body) throws ExecutionException, RetryException {
        Traces traces = ActiveTraces.getLocal();
        traces.add("WebhookLeader.makeCall start");
        RecurringTrace recurringTrace = new RecurringTrace("WebhookLeader.makeCall start");
        traces.add(recurringTrace);
        retryer.call(() -> {
            ActiveTraces.setLocal(traces);
            if (webhook.getTtlMinutes() > 0) {
                DateTime ttlTime = TimeUtil.now().minusMinutes(webhook.getTtlMinutes());
                if (contentPath.getTime().isBefore(ttlTime)) {
                    throw new ItemExpiredException(contentPath.toUrl() + " is before " + ttlTime);
                }
            }
            if (!leadership.hasLeadership()) {
                logger.debug("not leader {} {} {}", webhook.getCallbackUrl(), webhook.getName(), contentPath);
                return null;
            }
            String entity = body.toString();
            logger.debug("calling {} {} {}", webhook.getCallbackUrl(), contentPath, entity);
            ClientResponse clientResponse = client.resource(webhook.getCallbackUrl())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, entity);
            recurringTrace.update("WebhookLeader.makeCall completed", clientResponse);
            return clientResponse;
        });
    }

    void exit(boolean delete) {
        String name = webhook.getName();
        logger.info("exiting webhook " + name + " deleting " + delete);
        deleteOnExit.set(delete);
        closeStrategy();
        stopExecutor();
        curatorLeader.close();
        logger.info("exited webhook " + name + " deleting " + delete);
    }

    private void stopExecutor() {
        if (executorService == null) {
            return;
        }
        String name = webhook.getName();
        try {
            executorService.shutdown();
            logger.debug("awating termination " + name);
            executorService.awaitTermination(130, TimeUnit.SECONDS);
            logger.debug("stopped Executor " + name);
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
        return "/GroupLeader/" + webhook.getName();
    }

    private void delete() {
        String name = webhook.getName();
        logger.info("deleting " + name);
        webhookInProcess.delete(name);
        lastContentPath.delete(name, WEBHOOK_LAST_COMPLETED);
        webhookError.delete(name);
        logger.info("deleted " + name);
    }

    boolean deleteIfReady() {
        if (isReadyToDelete()) {
            deleteAnyway();
            return true;
        }
        return false;
    }

    void deleteAnyway() {
        try {
            debugLeaderPath();
            curator.delete().deletingChildrenIfNeeded().forPath(getLeaderPath());
        } catch (Exception e) {
            logger.warn("unable to delete leader path " + webhook.getName(), e);
        }
        delete();
    }

    private void debugLeaderPath() {
        try {
            String leaderPath = getLeaderPath();
            List<String> children = curator.getChildren().forPath(leaderPath);
            for (String child : children) {
                String path = leaderPath + "/" + child;
                byte[] bytes = curator.getData().forPath(path);
                logger.info("found child {} {} ", new String(bytes), path);
            }
        } catch (KeeperException.NoNodeException ignore) {
            //do nothing
        } catch (Exception e) {
            logger.info("unexpected exception " + webhook.getName(), e);
        }
    }

    private boolean isReadyToDelete() {
        try {
            List<String> children = curator.getChildren().forPath(getLeaderPath());
            return children.isEmpty();
        } catch (KeeperException.NoNodeException ignore) {
            return true;
        } catch (Exception e) {
            logger.warn("unexpected exception " + webhook.getName(), e);
            return true;
        }
    }

    public List<String> getErrors() {
        return webhookError.get(webhook.getName());
    }

    List<ContentPath> getInFlight(Webhook webhook) {
        return new ArrayList<>(new TreeSet<>(webhookInProcess.getSet(this.webhook.getName(), WebhookStrategy.createContentPath(webhook))));
    }

    public Webhook getWebhook() {
        return webhook;
    }
}
