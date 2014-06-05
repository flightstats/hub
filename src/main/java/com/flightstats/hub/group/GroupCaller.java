package com.flightstats.hub.group;

import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.cluster.LongValue;
import com.flightstats.hub.model.ContentKey;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupCaller implements Leader {
    private final static Logger logger = LoggerFactory.getLogger(GroupCaller.class);

    private Group group;
    private CuratorLeader curatorLeader;
    private final CuratorFramework curator;
    private final Provider<CallbackIterator> iteratorProvider;
    private Client client;
    private LongValue lastCompleted;
    private final Retryer<ClientResponse> retryer;

    @Inject
    public GroupCaller(CuratorFramework curator, Provider<CallbackIterator> iteratorProvider) {
        this.curator = curator;
        this.iteratorProvider = iteratorProvider;
        retryer = buildRetryer(1000);
    }

    public boolean tryLeadership(Group group) {
        logger.debug("starting group: " + group);
        this.group = group;
        lastCompleted = new LongValue(getValuePath(), ContentKey.START_VALUE, curator);
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        long start = lastCompleted.get();
        this.client = createClient();

        logger.debug("last completed at " + start + " " + group.getName());
        try (CallbackIterator iterator = iteratorProvider.get()) {
            iterator.start(start, group);
            while (iterator.hasNext() && hasLeadership.get()) {
                long next = iterator.next();
                if (group.isTransactional()) {
                    sendTransactional(next);
                } else {
                    sendAsynch(next);
                }
            }

        } finally {
            logger.info("stopping " + group);
        }
    }

    private void sendAsynch(long next) {
        //todo - gfm - 6/3/14 - add in retry behavior
        //todo - gfm - 6/3/14 - should this drop missed calls, or retry?  retries could back up if the service is down
        //todo - gfm - 6/3/14 - maybe set the threadpool limit on the Client, or use a Semaphore
        /*client.asyncResource(group.getCallbackUrl()).post("" + next);
        lastCompleted.update(next);*/
    }

    private void sendTransactional(long next) {
        try {
            logger.debug("sending {} to {}", next, group.getName());
            final GroupResponse groupResponse = new GroupResponse();
            groupResponse.add(group.getChannelUrl() + "/" + next);
            retryer.call(new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return client.resource(group.getCallbackUrl())
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .post(ClientResponse.class, groupResponse.toJson());
                }
            });
            lastCompleted.update(next);
            logger.debug("completed {} call to {} ", next, group.getName());
        } catch (Exception e) {
            //todo - gfm - 6/5/14 - can we ever get here?
            logger.warn("unable to send " + next + " to " + group, e);
        }
    }

    //todo - gfm - 6/3/14 - exit

    //todo - gfm - 6/5/14 - make sure ZK values are deleted when Group is deleted.

    private String getLeaderPath() {
        return "/GroupLeader/" + group.getName();
    }

    private String getValuePath() {
        return "/GroupLastCompleted/" + group.getName();
    }

    private Client createClient() {
        Client client = Client.create();
        client.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30));
        client.setReadTimeout((int) TimeUnit.SECONDS.toMillis(120));
        client.setFollowRedirects(true);
        if (!group.isTransactional()) {
            client.setExecutorService(new ThreadPoolExecutor(0, 5, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
        }
        return client;
    }

    public long getLastCompleted() {
        return lastCompleted.get();
    }

    //todo - gfm - 6/5/14 - test this (how)?
    static Retryer<ClientResponse> buildRetryer(int multiplier) {
        return RetryerBuilder.<ClientResponse>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable throwable) {
                        if (throwable != null) {
                            logger.info("got throwable trying to call client back ", throwable);
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
