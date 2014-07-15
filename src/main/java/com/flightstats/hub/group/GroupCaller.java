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
import com.github.rholder.retry.*;
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
    private final Retryer<ClientResponse> retryer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean deleteOnExit = new AtomicBoolean();

    private Group group;
    private CuratorLeader curatorLeader;
    private Client client;
    private LongValue lastCompleted;
    private ExecutorService executorService;
    private Semaphore semaphore;
    private LongSet inFlight;

    @Inject
    public GroupCaller(CuratorFramework curator, Provider<CallbackIterator> iteratorProvider,
                       SequenceLastUpdatedDao sequenceDao, GroupService groupService, MetricsTimer metricsTimer) {
        this.curator = curator;
        this.iteratorProvider = iteratorProvider;
        this.sequenceDao = sequenceDao;
        this.groupService = groupService;
        this.metricsTimer = metricsTimer;
        lastCompleted = new LongValue(curator);
        retryer = buildRetryer(1000);
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        executorService = Executors.newCachedThreadPool();
        semaphore = new Semaphore(group.getParallelCalls());
        lastCompleted.initialize(getValuePath(), getLastUpdated(group).getSequence());
        inFlight = new LongSet(getInFlightPath(), curator);
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    private ContentKey getLastUpdated(Group group) {
        return sequenceDao.getLastUpdated(ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl()));
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        logger.info("taking leadership " + group);
        Optional<Group> foundGroup = groupService.getGroup(group.getName());
        if (!foundGroup.isPresent()) {
            logger.info("group is missing, exiting " + group.getName());
            return;
        }
        this.client = GroupClient.createClient();

        try (CallbackIterator iterator = iteratorProvider.get()) {
            sendInFlight();
            long start = lastCompleted.get(getValuePath(), getLastUpdated(group).getSequence());

            logger.debug("last completed at {} {}", start, group.getName());
            iterator.start(start, group);
            while (iterator.hasNext() && hasLeadership.get()) {
                send(iterator.next());
            }
        } catch (RuntimeInterruptedException e) {
            logger.info("saw RuntimeInterruptedException for " + group.getName());
        } finally {
            logger.info("stopping " + group);
            if (deleteOnExit.get()) {
                delete();
            }
            //todo - gfm - 6/26/14 - does this need to stop the executorService?
        }
    }

    private void sendInFlight() {
        Set<Long> inFlightSet = inFlight.getSet();
        logger.debug("sending in flight {}", inFlightSet);
        for (Long inFlight : inFlightSet) {
            send(inFlight);
        }
    }

    private void send(final long next) {
        try {
            logger.debug("sending {} to {}", next, group.getName());
            //todo - gfm - 6/26/14 - should this InterruptedException propagate up?
            semaphore.acquire();
            executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    inFlight.add(next);
                    makeTimedCall(createResponse(next));
                    //todo - gfm - 6/22/14 - make sure lastCompleted only increases the value
                    lastCompleted.update(next, getValuePath());
                    inFlight.remove(next);
                    semaphore.release();
                    return null;
                }
            });

            logger.debug("completed {} call to {} ", next, group.getName());
        } catch (Exception e) {
            //todo - gfm - 6/26/14 - can we get here?
            logger.warn("unable to send " + next + " to " + group, e);
        }
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
        logger.debug("calling {} {}", group.getCallbackUrl(), group.getName());
        retryer.call(new Callable<ClientResponse>() {
            @Override
            public ClientResponse call() throws Exception {
                return client.resource(group.getCallbackUrl())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse.class, response.toString());
            }
        });
    }

    public void exit(boolean delete) {
        logger.info("exiting group " + group);
        deleteOnExit.set(delete);
        curatorLeader.close();
        try {
            executorService.shutdown();
            //todo - gfm - 6/27/14 - should this wait here?
            executorService.awaitTermination(1, TimeUnit.MINUTES);
            logger.info("stopped group " + group);
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
    }

    static Retryer<ClientResponse> buildRetryer(int multiplier) {
        return RetryerBuilder.<ClientResponse>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable throwable) {
                        //todo - gfm - 6/22/14 - should this handle InterruptedException separately?
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
                .withWaitStrategy(WaitStrategies.exponentialWait(multiplier, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.neverStop())
                .build();
    }
}
