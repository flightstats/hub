package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsTimer;
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

public class GroupLeader implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(GroupLeader.class);
    public static final String GROUP_LAST_COMPLETED = "/GroupLastCompleted/";

    private static final Client client = RestClient.createClient(60, 120, true);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();
    private final double keepLeadershipRate = HubProperties.getProperty("group.keepLeadershipRate", 0.75);

    @Inject
    private CuratorFramework curator;
    @Inject
    private ChannelService channelService;
    @Inject
    private GroupService groupService;
    @Inject
    private MetricsTimer metricsTimer;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private GroupContentPathSet groupInProcess;
    @Inject
    private GroupError groupError;

    private Group group;
    private CuratorLeader curatorLeader;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private AtomicBoolean hasLeadership;
    private Retryer<ClientResponse> retryer;

    private boolean exited = false;
    private GroupStrategy groupStrategy;
    private AtomicReference<ContentPath> lastUpdated = new AtomicReference();

    @Inject
    public GroupLeader() {
        logger.info("keep leadership rate {}", keepLeadershipRate);
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        curatorLeader = new CuratorLeader(getLeaderPath(), this);
        if (!group.isPaused()) {
            curatorLeader.start();
        } else {
            logger.info("not starting paused group " + group);
        }
        return true;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
        Optional<Group> foundGroup = groupService.getGroup(group.getName());
        if (!foundGroup.isPresent()) {
            Sleeper.sleep(1000);
            logger.info("group is missing, exiting " + group.getName());
            return;
        }
        this.group = foundGroup.get();
        logger.info("taking leadership " + group);
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(group.getParallelCalls());
        retryer = GroupRetryer.buildRetryer(group, groupError, hasLeadership);
        groupStrategy = GroupStrategy.getStrategy(group, lastContentPath, channelService);
        try {
            ContentPath lastCompletedPath = groupStrategy.getStartingPath();
            lastUpdated.set(lastCompletedPath);
            logger.info("last completed at {} {}", lastCompletedPath, group.getName());
            if (hasLeadership.get()) {
                sendInProcess(lastCompletedPath);
                groupStrategy.start(group, lastCompletedPath);
                while (hasLeadership.get()) {
                    Optional<ContentPath> nextOptional = groupStrategy.next();
                    if (nextOptional.isPresent()) {
                        send(nextOptional.get());
                    }
                }
            }
        } catch (RuntimeInterruptedException | InterruptedException e) {
            logger.info("saw InterruptedException for " + group.getName());
        } finally {
            logger.info("stopping last completed at {} {}", groupStrategy.getLastCompleted(), group.getName());
            hasLeadership.set(false);
            closeStrategy();
            if (deleteOnExit.get()) {
                delete();
            }
            stopExecutor();
            logger.info("stopped last completed at {} {}", groupStrategy.getLastCompleted(), group.getName());
            groupStrategy = null;
            executorService = null;
        }
    }

    @Override
    public double keepLeadershipRate() {
        return keepLeadershipRate;
    }

    private void sendInProcess(ContentPath lastCompletedPath) throws InterruptedException {
        Set<ContentPath> inProcessSet = groupInProcess.getSet(group.getName(), lastCompletedPath);
        logger.debug("sending in process {} to {}", inProcessSet, group.getName());
        for (ContentPath toSend : inProcessSet) {
            if (toSend.compareTo(lastCompletedPath) < 0) {
                ActiveTraces.start("GroupLeader inProcess", group);
                ContentPath contentPath;
                try {
                    contentPath = groupStrategy.inProcess(toSend);
                } finally {
                    ActiveTraces.end();
                }
                send(contentPath);
            } else {
                groupInProcess.remove(group.getName(), toSend);
            }
        }
    }

    private void send(ContentPath contentPath) throws InterruptedException {
        semaphore.acquire();
        logger.trace("sending {} to {}", contentPath, group.getName());
        executorService.submit(new Callable<Object>() {
            @Trace(metricName = "GroupCaller", dispatcher = true)
            @Override
            public Object call() throws Exception {
                ActiveTraces.start("GroupLeader.send", group, contentPath);
                groupInProcess.add(group.getName(), contentPath);
                try {
                    long delta = System.currentTimeMillis() - contentPath.getTime().getMillis();
                    metricsTimer.send("group." + group.getName() + ".delta", delta);
                    makeTimedCall(contentPath, groupStrategy.createResponse(contentPath, mapper));
                    completeCall(contentPath);
                    logger.trace("completed {} call to {} ", contentPath, group.getName());
                } catch (RetryException e) {
                    logger.info("exception sending {} to {} {} ", contentPath, group.getName(), e.getMessage());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ItemExpiredException) {
                        logger.info("stopped trying {} to {} {} ", contentPath, group.getName(), cause.getMessage());
                        completeCall(contentPath);
                    }
                } catch (Exception e) {
                    logger.warn("exception sending " + contentPath + " to " + group.getName(), e);
                } finally {
                    semaphore.release();
                    ActiveTraces.end();
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
            lastContentPath.updateIncrease(contentPath, group.getName(), GROUP_LAST_COMPLETED);
        }
        groupInProcess.remove(group.getName(), contentPath);
    }

    private void makeTimedCall(ContentPath contentPath, ObjectNode body) throws Exception {
        metricsTimer.time("group." + group.getName() + ".post",
                () -> {
                    makeCall(contentPath, body);
                    return null;
                });
    }

    private void makeCall(ContentPath contentPath, ObjectNode body) throws ExecutionException, RetryException {
        Traces traces = ActiveTraces.getLocal();
        traces.add("GroupLeader.makeCall start");
        RecurringTrace recurringTrace = new RecurringTrace("GroupLeader.makeCall start");
        traces.add(recurringTrace);
        retryer.call(() -> {
            ActiveTraces.setLocal(traces);
            if (group.getTtlMinutes() > 0) {
                DateTime ttlTime = TimeUtil.now().minusMinutes(group.getTtlMinutes());
                if (contentPath.getTime().isBefore(ttlTime)) {
                    throw new ItemExpiredException(contentPath.toUrl() + " is before " + ttlTime);
                }
            }
            if (!hasLeadership.get()) {
                logger.debug("not leader {} {} {}", group.getCallbackUrl(), group.getName(), contentPath);
                return null;
            }
            logger.debug("calling {} {} {}", group.getCallbackUrl(), contentPath);
            ClientResponse clientResponse = client.resource(group.getCallbackUrl())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, body.toString());
            recurringTrace.update("GroupLeader.makeCall completed", clientResponse);
            return clientResponse;
        });
    }

    public void exit(boolean delete) {
        String name = group.getName();
        logger.info("exiting group " + name + " deleting " + delete);
        deleteOnExit.set(delete);
        curatorLeader.close();
        closeStrategy();
        stopExecutor();
    }

    private void stopExecutor() {
        if (executorService == null) {
            return;
        }
        String name = group.getName();
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
            if (groupStrategy != null) {
                groupStrategy.close();
            }
        } catch (Exception e) {
            logger.warn("unable to close callbackQueue", e);
        }
    }

    private String getLeaderPath() {
        return "/GroupLeader/" + group.getName();
    }

    private void delete() {
        logger.info("deleting " + group.getName());
        groupInProcess.delete(group.getName());
        lastContentPath.delete(group.getName(), GROUP_LAST_COMPLETED);
        groupError.delete(group.getName());
        logger.info("deleted " + group.getName());
    }

    public boolean deleteIfReady() {
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
            logger.warn("unable to delete leader path " + group.getName(), e);
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
            logger.info("unexpected exception " + group.getName(), e);
        }
    }

    private boolean isReadyToDelete() {
        try {
            List<String> children = curator.getChildren().forPath(getLeaderPath());
            return children.isEmpty();
        } catch (KeeperException.NoNodeException ignore) {
            return true;
        } catch (Exception e) {
            logger.warn("unexpected exception " + group.getName(), e);
            return true;
        }
    }

    public List<String> getErrors() {
        return groupError.get(group.getName());
    }

    public List<ContentPath> getInFlight(Group group) {
        return new ArrayList<>(new TreeSet<>(groupInProcess.getSet(this.group.getName(), GroupStrategy.createContentPath(group))));
    }

    public Group getGroup() {
        return group;
    }
}
