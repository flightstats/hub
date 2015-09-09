package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.newrelic.api.agent.Trace;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupCaller implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(GroupCaller.class);
    public static final String GROUP_LAST_COMPLETED = "/GroupLastCompleted/";

    private static final Client client = RestClient.createClient(30, 120, true);
    private final CuratorFramework curator;
    private final ChannelService channelService;
    private final GroupService groupService;
    private final MetricsTimer metricsTimer;
    private final LastContentPath lastContentPath;
    private final GroupContentPathSet groupInProcess;
    private final GroupError groupError;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    private Group group;
    private CuratorLeader curatorLeader;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private AtomicBoolean hasLeadership;
    private Retryer<ClientResponse> retryer;

    private boolean exited = false;
    private Caller caller;

    @Inject
    public GroupCaller(CuratorFramework curator, ChannelService channelService,
                       GroupService groupService, MetricsTimer metricsTimer,
                       LastContentPath LastContentPath, GroupContentPathSet groupInProcess, GroupError groupError) {
        this.curator = curator;
        this.channelService = channelService;
        this.groupService = groupService;
        this.metricsTimer = metricsTimer;
        this.lastContentPath = LastContentPath;
        this.groupInProcess = groupInProcess;
        this.groupError = groupError;
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(group.getParallelCalls());
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
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
        retryer = GroupRetryer.buildRetryer(group.getName(), groupError, hasLeadership);
        logger.info("taking leadership " + group);
        Optional<Group> foundGroup = groupService.getGroup(group.getName());
        if (!foundGroup.isPresent()) {
            Sleeper.sleep(1000);
            logger.info("group is missing, exiting " + group.getName());
            return;
        }
        this.group = foundGroup.get();
        //todo - gfm - 9/9/15 - add option for batch caller
        caller = new SingleCaller(group, lastContentPath, channelService);
        try {
            ContentPath lastCompletedKey = caller.getStartingPath();
            logger.info("last completed at {} {}", lastCompletedKey, group.getName());
            if (hasLeadership.get()) {
                sendInProcess(lastCompletedKey);
                caller.start(group, lastCompletedKey);
                while (hasLeadership.get()) {
                    Optional<ContentPath> nextOptional = caller.next();
                    if (nextOptional.isPresent()) {
                        send(nextOptional.get());
                    }
                }
            }
        } catch (RuntimeInterruptedException | InterruptedException e) {
            logger.info("saw InterruptedException for " + group.getName());
        } finally {
            logger.info("stopping last completed at {} {}", caller.getLastCompleted(), group.getName());
            closeQueue();
            if (deleteOnExit.get()) {
                delete();
            }
        }
    }

    private void sendInProcess(ContentPath lastCompletedKey) throws InterruptedException {
        Set<ContentPath> inProcessSet = groupInProcess.getSet(group.getName(), lastCompletedKey);
        logger.trace("sending in process {} to {}", inProcessSet, group.getName());
        for (ContentPath toSend : inProcessSet) {
            if (toSend.compareTo(lastCompletedKey) < 0) {
                send(toSend);
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
                groupInProcess.add(group.getName(), contentPath);
                try {
                    makeTimedCall(caller.createResponse(contentPath, mapper));
                    lastContentPath.updateIncrease(contentPath, group.getName(), GROUP_LAST_COMPLETED);
                    groupInProcess.remove(group.getName(), contentPath);
                    logger.trace("completed {} call to {} ", contentPath, group.getName());
                } catch (Exception e) {
                    logger.warn("exception sending " + contentPath + " to " + group.getName(), e);
                } finally {
                    semaphore.release();
                }
                return null;
            }
        });
    }

    private void makeTimedCall(final ObjectNode response) throws Exception {
        metricsTimer.time("group." + group.getName() + ".post",
                () -> metricsTimer.time("group.ALL.post",
                        () -> {
                            makeCall(response);
                            return null;
                        }));
    }

    private void makeCall(final ObjectNode response) throws ExecutionException, RetryException {
        retryer.call(() -> {
            if (!hasLeadership.get()) {
                logger.debug("not leader {} {} {}", group.getCallbackUrl(), group.getName(), response);
                return null;
            }
            String postId = UUID.randomUUID().toString();
            logger.debug("calling {} {} {}", group.getCallbackUrl(), response, postId);
            return client.resource(group.getCallbackUrl())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header("post-id", postId)
                    .post(ClientResponse.class, response.toString());
        });
    }

    public void exit(boolean delete) {
        String name = group.getName();
        logger.info("exiting group " + name + " deleting " + delete);
        deleteOnExit.set(delete);
        curatorLeader.close();
        closeQueue();
        try {
            executorService.shutdown();
            logger.info("awating termination " + name);
            executorService.awaitTermination(1, TimeUnit.SECONDS);
            logger.info("terminated " + name);
        } catch (InterruptedException e) {
            logger.warn("unable to stop?" + name, e);
        }
    }

    private void closeQueue() {
        try {
            if (caller != null) {
                caller.close();
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
        return new ArrayList<>(new TreeSet<>(groupInProcess.getSet(this.group.getName(), Caller.getType(group))));
    }

    public Group getGroup() {
        return group;
    }
}
