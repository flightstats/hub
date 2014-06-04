package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.api.CuratorEvent;
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
    private static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(ReplicatorImpl.class);

    private final ReplicationService replicationService;
    private final Provider<DomainReplicator> domainReplicatorProvider;
    private final WatchManager watchManager;
    private final Map<String, DomainReplicator> replicatorMap = new HashMap<>();

    @Inject
    public ReplicatorImpl(ReplicationService replicationService,
                          Provider<DomainReplicator> domainReplicatorProvider, WatchManager watchManager) {
        this.replicationService = replicationService;
        this.domainReplicatorProvider = domainReplicatorProvider;
        this.watchManager = watchManager;
        HubServices.register(new ReplicatorImplService());
    }

    private class ReplicatorImplService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startReplicator();
        }

        @Override
        protected void shutDown() throws Exception { }

    }

    public void startReplicator() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                replicateDomains();
            }

            @Override
            public String getPath() {
                return REPLICATOR_WATCHER_PATH;
            }
        });
        replicateDomains();
    }

    private synchronized void replicateDomains() {
        logger.info("replicating domains");
        Collection<ReplicationDomain> domains = replicationService.getDomains(true);
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

    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
    }

}
