package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("Convert2Lambda")
public class GroupCaller implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(GroupCaller.class);

    private final CuratorFramework curator;
    private final Provider<CallbackQueue> queueProvider;
    private final GroupService groupService;
    private final MetricsTimer metricsTimer;
    private final GroupContentKey groupContentKey;
    private final GroupContentKeySet groupInProcess;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    private Group group;
    private CuratorLeader curatorLeader;
    private Client client;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private AtomicBoolean hasLeadership;
    private Retryer<ClientResponse> retryer;
    private CallbackQueue callbackQueue;

    @Inject
    public GroupCaller(CuratorFramework curator, Provider<CallbackQueue> queueProvider,
                       GroupService groupService, MetricsTimer metricsTimer,
                       GroupContentKey groupContentKey, GroupContentKeySet groupInProcess) {
        this.curator = curator;
        this.queueProvider = queueProvider;
        this.groupService = groupService;
        this.metricsTimer = metricsTimer;
        this.groupContentKey = groupContentKey;
        this.groupInProcess = groupInProcess;
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(group.getParallelCalls());
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
        retryer = buildRetryer();
        logger.info("taking leadership " + group);
        Optional<Group> foundGroup = groupService.getGroup(group.getName());
        /**
         * todo - gfm - 12/21/14 - the issue with integration tests and deleting
         * is that a second server picks up leadership after the first has lost it and after the group is deleted
         * hub-03

         INFO 2014-12-22 01:06:34,060 [qtp1973790994-493] com.flightstats.hub.group.GroupService [line 35] - upsert group Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=null, name=test_0_5755902747623622)
         DEBUG 2014-12-22 01:06:34,086 [pool-7-thread-9] com.flightstats.hub.group.GroupCaller [line 70] - starting group: Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=1, name=test_0_5755902747623622)
         INFO 2014-12-22 01:06:34,094 [LeaderSelector-556] com.flightstats.hub.cluster.CuratorLeader [line 59] - have leadership for /GroupLeader/test_0_5755902747623622
         INFO 2014-12-22 01:06:34,094 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 83] - taking leadership Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=1, name=test_0_5755902747623622)
         DEBUG 2014-12-22 01:06:34,176 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 93] - last completed at 2014/12/22/01/06/34/067/Rf06Pm test_0_5755902747623622
         INFO 2014-12-22 01:06:34,177 [LeaderSelector-556] com.flightstats.hub.group.GroupContentKeySet [line 53] - no node for /GroupInFlight/test_0_5755902747623622
         INFO 2014-12-22 01:06:34,474 [qtp1973790994-797] com.flightstats.hub.group.GroupService [line 82] - deleting group test_0_5755902747623622
         INFO 2014-12-22 01:06:34,488 [qtp1973790994-797] com.flightstats.hub.group.GroupCallbackImpl [line 112] - waiting to delete test_0_5755902747623622
         INFO 2014-12-22 01:06:34,496 [pool-7-thread-7] com.flightstats.hub.group.GroupCallbackImpl [line 71] - stopping groups [test_0_5755902747623622]
         INFO 2014-12-22 01:06:34,496 [pool-7-thread-7] com.flightstats.hub.group.GroupCallbackImpl [line 73] - stopping test_0_5755902747623622
         INFO 2014-12-22 01:06:34,497 [pool-506-thread-1] com.flightstats.hub.group.GroupCaller [line 192] - exiting group test_0_5755902747623622 deleting true
         INFO 2014-12-22 01:06:34,497 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 106] - saw InterruptedException for test_0_5755902747623622
         INFO 2014-12-22 01:06:34,497 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 108] - stopping Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=1, name=test_0_5755902747623622)
         INFO 2014-12-22 01:06:34,497 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 225] - deleting test_0_5755902747623622
         WARN 2014-12-22 01:06:34,497 [LeaderSelector-556] com.flightstats.hub.group.GroupContentKeySet [line 73] - unable to delete /GroupInFlight/test_0_5755902747623622 null
         WARN 2014-12-22 01:06:34,498 [LeaderSelector-556] com.flightstats.hub.group.GroupContentKey [line 91] - unable to delete /GroupLastCompleted/test_0_5755902747623622 null
         INFO 2014-12-22 01:06:34,498 [LeaderSelector-556] com.flightstats.hub.group.GroupCaller [line 228] - deleted test_0_5755902747623622
         INFO 2014-12-22 01:06:34,498 [LeaderSelector-556] com.flightstats.hub.cluster.CuratorLeader [line 68] - lost leadership /GroupLeader/test_0_5755902747623622
         INFO 2014-12-22 01:06:34,499 [pool-506-thread-1] com.flightstats.hub.group.GroupCaller [line 198] - awating termination test_0_5755902747623622
         INFO 2014-12-22 01:06:34,499 [pool-506-thread-1] com.flightstats.hub.group.GroupCaller [line 200] - terminated test_0_5755902747623622
         INFO 2014-12-22 01:06:35,489 [qtp1973790994-797] com.flightstats.hub.group.GroupCallbackImpl [line 112] - waiting to delete test_0_5755902747623622
         INFO 2014-12-22 01:06:37,490 [qtp1973790994-797] com.flightstats.hub.group.GroupCallbackImpl [line 112] - waiting to delete test_0_5755902747623622
         INFO 2014-12-22 01:06:40,491 [qtp1973790994-797] com.flightstats.hub.group.GroupCallbackImpl [line 112] - waiting to delete test_0_5755902747623622
         INFO 2014-12-22 01:06:44,492 [qtp1973790994-797] com.flightstats.hub.group.GroupCallbackImpl [line 112] - waiting to delete test_0_5755902747623622
         *
         * hub-02
         DEBUG 2014-12-22 01:06:34,094 [pool-7-thread-2] com.flightstats.hub.group.GroupCaller [line 70] - starting group: Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=1, name=test_0_5755902747623622)
         INFO 2014-12-22 01:06:34,502 [LeaderSelector-538] com.flightstats.hub.cluster.CuratorLeader [line 59] - have leadership for /GroupLeader/test_0_5755902747623622
         INFO 2014-12-22 01:06:34,502 [LeaderSelector-538] com.flightstats.hub.group.GroupCaller [line 83] - taking leadership Group(callbackUrl=http://nothing/callback, channelUrl=http://nothing/channel/notHere, parallelCalls=1, name=test_0_5755902747623622)
         INFO 2014-12-22 01:06:34,517 [LeaderSelector-538] com.flightstats.hub.group.GroupCaller [line 86] - group is missing, exiting test_0_5755902747623622
         INFO 2014-12-22 01:06:34,517 [LeaderSelector-538] com.flightstats.hub.cluster.CuratorLeader [line 68] - lost leadership /GroupLeader/test_0_5755902747623622
         INFO 2014-12-22 01:06:34,520 [pool-7-thread-3] com.flightstats.hub.group.GroupCallbackImpl [line 71] - stopping groups [test_0_5755902747623622]
         INFO 2014-12-22 01:06:34,521 [pool-7-thread-3] com.flightstats.hub.group.GroupCallbackImpl [line 73] - stopping test_0_5755902747623622
         INFO 2014-12-22 01:06:34,521 [pool-483-thread-1] com.flightstats.hub.group.GroupCaller [line 192] - exiting group test_0_5755902747623622 deleting true
         INFO 2014-12-22 01:06:34,523 [pool-483-thread-1] com.flightstats.hub.group.GroupCaller [line 198] - awating termination test_0_5755902747623622
         INFO 2014-12-22 01:06:34,523 [pool-483-thread-1] com.flightstats.hub.group.GroupCaller [line 200] - terminated test_0_5755902747623622
         */
        if (!foundGroup.isPresent()) {
            logger.info("group is missing, exiting " + group.getName());
            return;
        }
        this.client = GroupClient.createClient();
        callbackQueue = queueProvider.get();
        try {
            ContentKey lastCompletedKey = groupContentKey.get(group.getName(), new ContentKey());
            logger.debug("last completed at {} {}", lastCompletedKey, group.getName());
            if (hasLeadership.get()) {
                sendInProcess(lastCompletedKey);
                callbackQueue.start(group, lastCompletedKey);
                while (hasLeadership.get()) {
                    Optional<ContentKey> nextOptional = callbackQueue.next();
                    if (nextOptional.isPresent()) {
                        send(nextOptional.get());
                    }

                }
            }
        } catch (RuntimeInterruptedException | InterruptedException e) {
            logger.info("saw InterruptedException for " + group.getName());
        } finally {
            logger.info("stopping " + group);
            closeQueue();
            if (deleteOnExit.get()) {
                delete();
            }
        }
    }

    private void sendInProcess(ContentKey lastCompletedKey) throws InterruptedException {
        Set<ContentKey> inProcessSet = groupInProcess.getSet(group.getName());
        logger.trace("sending in process {} to {}", inProcessSet, group.getName());
        for (ContentKey toSend : inProcessSet) {
            if (toSend.compareTo(lastCompletedKey) < 0) {
                send(toSend);
            } else {
                groupInProcess.remove(group.getName(), toSend);
            }
        }
    }
    private void send(ContentKey key) throws InterruptedException {
        logger.trace("sending {} to {}", key, group.getName());
        semaphore.acquire();
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                groupInProcess.add(group.getName(), key);
                try {
                    makeTimedCall(createResponse(key));
                    groupContentKey.updateIncrease(key, group.getName());
                    groupInProcess.remove(group.getName(), key);
                    logger.trace("completed {} call to {} ", key, group.getName());
                } catch (Exception e) {
                    logger.warn("exception sending " + key + " to " + group.getName(), e);
                } finally {
                    semaphore.release();
                }
                return null;
            }
        });
    }

    private ObjectNode createResponse(ContentKey key) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", group.getName());
        ArrayNode uris = response.putArray("uris");
        uris.add(group.getChannelUrl() + "/" + key.toUrl());
        return response;
    }

    private void makeTimedCall(final ObjectNode response) throws Exception {
        metricsTimer.time("group." + group.getName() + ".post", new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return metricsTimer.time("group.ALL.post", new Callable<Object>() {
                    @Override
                    public Object call() throws ExecutionException, RetryException {
                        makeCall(response);
                        return null;
                    }
                });
            }
        });
    }

    private void makeCall(final ObjectNode response) throws ExecutionException, RetryException {
        retryer.call(new Callable<ClientResponse>() {
            @Override
            public ClientResponse call() throws Exception {
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
            }
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
            logger.warn("unable to stop?", e);
        }
    }

    private void closeQueue() {
        try {
            if (callbackQueue != null) {
                callbackQueue.close();
            }
        } catch (Exception e) {
            logger.warn("unable to close callbackQueue", e);
        }
    }

    private String getLeaderPath() {
        return "/GroupLeader/" + group.getName();
    }

    public ContentKey getLastCompleted() {
        return groupContentKey.get(group.getName(), ContentKey.NONE);
    }

    private void delete() {
        logger.info("deleting " + group.getName());
        groupInProcess.delete(group.getName());
        groupContentKey.delete(group.getName());
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
            debugLaederPath();
            curator.delete().deletingChildrenIfNeeded().forPath(getLeaderPath());
        } catch (Exception e) {
            logger.warn("unable to delete leader path " + group.getName(), e);
        }
        delete();
    }

    private void debugLaederPath() {
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

    private Retryer<ClientResponse> buildRetryer() {
        return RetryerBuilder.<ClientResponse>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable throwable) {
                        if (throwable != null) {
                            if (throwable.getClass().isAssignableFrom(ClientHandlerException.class)) {
                                logger.info("got ClientHandlerException trying to call client back " + throwable.getMessage());
                            } else {
                                logger.info("got throwable trying to call client back ", throwable);
                            }
                        }
                        return throwable != null;
                    }
                })
                .retryIfResult(new Predicate<ClientResponse>() {
                    @Override
                    public boolean apply(@Nullable ClientResponse response) {
                        if (response == null) return true;
                        try {
                            boolean failure = response.getStatus() != 200;
                            if (failure) {
                                logger.info("unable to send to " + response);
                            }
                            return failure;
                        } finally {
                            close(response);
                        }
                    }

                    private void close(ClientResponse response) {
                        try {
                            response.close();
                        } catch (ClientHandlerException e) {
                            logger.info("exception closing response", e);
                        }
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .withStopStrategy(new GroupStopStrategy(hasLeadership))
                .build();
    }
}
