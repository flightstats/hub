package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.cluster.LongSet;
import com.flightstats.hub.cluster.LongValue;
import com.flightstats.hub.dao.SequenceLastUpdatedDao;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.ChannelNameUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupCaller implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(GroupCaller.class);

    private final CuratorFramework curator;
    private final Provider<CallbackIterator> iteratorProvider;
    private final SequenceLastUpdatedDao sequenceDao;
    private final GroupService groupService;
    private final MetricsTimer metricsTimer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    private Group group;
    private CuratorLeader curatorLeader;
    private Client client;
    private LongValue lastCompleted;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private LongSet inProcess;
    private AtomicBoolean hasLeadership;
    private Retryer<ClientResponse> retryer;

    @Inject
    public GroupCaller(CuratorFramework curator, Provider<CallbackIterator> iteratorProvider,
                       SequenceLastUpdatedDao sequenceDao, GroupService groupService, MetricsTimer metricsTimer) {
        this.curator = curator;
        this.iteratorProvider = iteratorProvider;
        this.sequenceDao = sequenceDao;
        this.groupService = groupService;
        this.metricsTimer = metricsTimer;
        lastCompleted = new LongValue(curator);
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(group.getParallelCalls());
        lastCompleted.initialize(getValuePath(), getLastUpdated(group).getSequence());
        inProcess = new LongSet(getInFlightPath(), curator);
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    private ContentKey getLastUpdated(Group group) {
        return sequenceDao.getLastUpdated(ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl()));
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
        retryer = buildRetryer();
        logger.info("taking leadership " + group);
        Optional<Group> foundGroup = groupService.getGroup(group.getName());
        if (!foundGroup.isPresent()) {
            logger.info("group is missing, exiting " + group.getName());
            return;
        }
        this.client = GroupClient.createClient();

        try (CallbackIterator iterator = iteratorProvider.get()) {
            long lastCompletedId = lastCompleted.get(getValuePath(), getLastUpdated(group).getSequence());
            logger.debug("last completed at {} {}", lastCompletedId, group.getName());
            sendInProcess(lastCompletedId);
            iterator.start(lastCompletedId, group);
            while (iterator.hasNext() && hasLeadership.get()) {
                send(iterator.next());
            }
        } catch (RuntimeInterruptedException|InterruptedException e) {
            logger.info("saw InterruptedException for " + group.getName());
        } finally {
            logger.info("stopping " + group);
            if (deleteOnExit.get()) {
                delete();
            }
        }
    }

    private long sendInProcess(long lastCompletedId) throws InterruptedException {
        Set<Long> inProcessSet = inProcess.getSet();
        logger.debug("sending in process {} to {}", inProcessSet, group.getName());
        for (Long toSend : inProcessSet) {
            if (toSend < lastCompletedId) {
                send(toSend);
            } else {
                inProcess.remove(toSend);
            }
        }
        return lastCompletedId;
    }

    private void send(final long next) throws InterruptedException {
        logger.debug("sending {} to {}", next, group.getName());
        semaphore.acquire();
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                inProcess.add(next);
                try {
                    makeTimedCall(createResponse(next));
                    lastCompleted.updateIncrease(next, getValuePath());
                    inProcess.remove(next);
                    logger.debug("completed {} call to {} ", next, group.getName());
                } catch (Exception e) {
                    logger.warn("exception sending " + next + " to " + group.getName(), e);
                } finally {
                    semaphore.release();
                }
                return null;
            }
        });
    }

    private ObjectNode createResponse(long next) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", group.getName());
        ArrayNode uris = response.putArray("uris");
        uris.add(group.getChannelUrl() + "/" + next);
        return response;
    }

    private void makeTimedCall(final ObjectNode response) throws Exception {
        metricsTimer.time("group." + group.getName() + ".post", new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return metricsTimer.time("all-groups.post", new Callable<Object>() {
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
                logger.debug("calling {} {} {}", group.getCallbackUrl(), group.getName(), response);
                return client.resource(group.getCallbackUrl())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse.class, response.toString());
            }
        });
    }

    public void exit(boolean delete) {
        String name = group.getName();
        logger.info("exiting group " + name + " deleting " + delete);
        deleteOnExit.set(delete);
        curatorLeader.close();
        try {
            executorService.shutdown();
            logger.info("awating termination " + name);
            executorService.awaitTermination(90, TimeUnit.SECONDS);
            logger.info("terminated " + name);
        } catch (InterruptedException e) {
            logger.warn("unable to stop?", e);
        }
    }

    private String getLeaderPath() {
        return "/GroupLeader/" + group.getName();
    }

    private String getValuePath() {
        return "/GroupLastCompleted/" + group.getName();
    }

    private String getInFlightPath() {
        return "/GroupInFlight/" + group.getName();
    }

    public long getLastCompleted() {
        return lastCompleted.get(getValuePath(), 0);
    }

    private void delete() {
        logger.info("deleting " + group.getName());
        LongSet.delete(getInFlightPath(), curator);
        lastCompleted.delete(getValuePath());
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(getLeaderPath());
        } catch (Exception e) {
            logger.warn("unable to delete leader path", e);
        }
        logger.info("deleted " + group.getName());
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
                        boolean failure = response.getStatus() != 200;
                        if (failure) {
                            logger.info("unable to send to " + response);
                        }
                        return failure;
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .withStopStrategy(new GroupStopStrategy(hasLeadership))
                .build();
    }
}
