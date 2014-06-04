package com.flightstats.hub.group;

import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.model.SequenceContentKey;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Inject
    public GroupCaller(CuratorFramework curator, Provider<CallbackIterator> iteratorProvider) {
        this.curator = curator;
        this.iteratorProvider = iteratorProvider;

    }

    public boolean tryLeadership(Group group) {
        this.group = group;
        logger.debug("starting group: " + group);
        curatorLeader = new CuratorLeader(getLeaderPath(), this, curator);
        curatorLeader.start();
        return true;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        createNode();
        long start = getLastCompleted();
        this.client = createClient();

        logger.info("last completed at " + start);
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

    //todo - gfm - 6/3/14 - add in retry behavior

    private void sendAsynch(long next) {
        //todo - gfm - 6/3/14 - should this drop missed calls, or retry?  retries could back up if the service is down
        //todo - gfm - 6/3/14 - maybe set the threadpool limit on the Client, or use a Semaphore
        client.asyncResource(group.getCallbackUrl()).post("" + next);
        updateLatest(next);
    }

    private void sendTransactional(long next) {
        logger.debug("sending ", next);
        ClientResponse response = client.resource(group.getCallbackUrl()).post(ClientResponse.class, "" + next);
        if (response.getStatus() == 200) {
            //todo - gfm - 6/3/14 - this will skip items if we get a non-200 response code
            updateLatest(next);
            logger.info("set " + next);
        }
    }

    private void updateLatest(long next)  {
        try {
            curator.setData().forPath(getValuePath(), Longs.toByteArray(next));
        } catch (Exception e) {
            logger.warn("unable to set latest {} {}", group, next);
        }
    }

    //todo - gfm - 6/3/14 - exit

    private String getLeaderPath() {
        return "/GroupLeader/" + group.getName();
    }

    private String getValuePath() {
        return "/GroupLastCompleted/" + group.getName();
    }

    private long getLastCompleted() {
        try {
            return Longs.fromByteArray(curator.getData().forPath(getValuePath()));
        } catch (Exception e) {
            logger.warn("unable to get node" + e.getMessage());
            return SequenceContentKey.START_VALUE;
        }
    }

    private void createNode() {
        try {
            curator.create().creatingParentsIfNeeded().forPath(getValuePath(), Longs.toByteArray(SequenceContentKey.START_VALUE));
        } catch (KeeperException.NodeExistsException ignore ) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
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
}
