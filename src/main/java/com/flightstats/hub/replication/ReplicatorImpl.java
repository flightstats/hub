package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubServices;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Replication is moving from one Hub into another Hub
 * in Replication, we will presume we are moving forward in time, starting with configurable item age.
 * <p/>
 * Secnario:
 * Producers are inserting Items into a Hub channel
 * The Hub is setup to Replicate a channel from a Hub
 * Replication starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Replication stays up to date, with some minimal amount of lag
 */
public class ReplicatorImpl implements Replicator {
    public static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(ReplicatorImpl.class);

    private final ReplicationService replicationService;
    private final CuratorFramework curator;
    private final Provider<DomainReplicator> domainReplicatorProvider;
    private final Map<String, DomainReplicator> replicatorMap = new HashMap<>();

    @Inject
    public ReplicatorImpl(ReplicationService replicationService, CuratorFramework curator,
                          Provider<DomainReplicator> domainReplicatorProvider) {
        this.replicationService = replicationService;
        this.curator = curator;
        this.domainReplicatorProvider = domainReplicatorProvider;
        HubServices.register(new ReplicatorImplService());
    }

    private class ReplicatorImplService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            logger.info("startup");
            startReplicator();
        }

        @Override
        protected void shutDown() throws Exception { }


    }

    public void startReplicator() {
        logger.info("starting replicator");
        createNode();
        addListener();
        addWatcher();
        replicateDomains();
    }

    private void addListener() {
        curator.getCuratorListenable().addListener(new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                if (REPLICATOR_WATCHER_PATH.equals(event.getPath())) {
                    //todo - gfm - 1/29/14 - replicateDomains should probably happen in a separate thread.
                    replicateDomains();
                    addWatcher();
                }
            }
        });
    }

    private void addWatcher() {
        try {
            curator.getData().watched().forPath(REPLICATOR_WATCHER_PATH);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
    }

    private void createNode() {
        try {
            curator.create().creatingParentsIfNeeded().forPath(REPLICATOR_WATCHER_PATH, Longs.toByteArray(System.currentTimeMillis()));
        } catch (KeeperException.NodeExistsException ignore ) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    private synchronized void replicateDomains() {
        logger.info("replicating domains");
        Collection<ReplicationDomain> domains = replicationService.getDomains();
        List<String> currentDomains = new ArrayList<>();
        for (ReplicationDomain domain : domains) {
            currentDomains.add(domain.getDomain());
            logger.info("domain " + domain);
            if (replicatorMap.containsKey(domain.getDomain())) {
                DomainReplicator domainReplicator = replicatorMap.get(domain.getDomain());
                if (domainReplicator.isDifferent(domain)) {
                    domainReplicator.exit();
                    startReplication(domain);
                }
            } else {
                startReplication(domain);
            }
        }
        logger.info("current domains " + currentDomains);
        Set<String> keys = new HashSet<>(replicatorMap.keySet());
        keys.removeAll(currentDomains);
        for (String key : keys) {
            logger.info("removing " + key);
            replicatorMap.get(key).exit();
            replicatorMap.remove(key);
        }
    }

    private void startReplication(ReplicationDomain domain) {
        logger.info("starting replication of " + domain.getDomain());
        DomainReplicator domainReplicator = domainReplicatorProvider.get();
        domainReplicator.start(domain);
        replicatorMap.put(domain.getDomain(), domainReplicator);
    }

    @Override
    public List<DomainReplicator> getDomainReplicators() {
        return Collections.unmodifiableList(new ArrayList<>(replicatorMap.values()));
    }

}
