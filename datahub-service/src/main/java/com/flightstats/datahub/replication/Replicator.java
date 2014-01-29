package com.flightstats.datahub.replication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.CuratorFramework;
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
public class Replicator {
    public static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(Replicator.class);

    private final ReplicationService replicationService;
    private final CuratorFramework curator;
    private final Provider<DomainReplicator> domainReplicatorProvider;
    //todo - gfm - 1/29/14 - domainReplicators should probably be replaced by replicatorMap
    private final List<DomainReplicator> domainReplicators = new ArrayList<>();
    private final Map<String, DomainReplicator> replicatorMap = new HashMap<>();

    @Inject
    public Replicator(ReplicationService replicationService, CuratorFramework curator,
                      Provider<DomainReplicator> domainReplicatorProvider) {
        this.replicationService = replicationService;
        this.curator = curator;
        this.domainReplicatorProvider = domainReplicatorProvider;
    }

    //todo - gfm - 1/29/14 - figure out startup scenario for this, should probably be done thru guice
    public void startThreads() {
        //todo - gfm - 1/28/14 - figure out watcher semantics
        try {
            curator.getData().watched().inBackground().forPath(REPLICATOR_WATCHER_PATH);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
        //todo - gfm - 1/29/14 - this code should get triggered when the replication config changes, aka by REPLICATOR_WATCHER_PATH
        replicateDomains();
    }

    private synchronized void replicateDomains() {
        Collection<ReplicationDomain> domains = replicationService.getDomains();
        for (ReplicationDomain domain : domains) {
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
    }

    private void startReplication(ReplicationDomain domain) {
        logger.info("starting replication of " + domain.getDomain());
        DomainReplicator domainReplicator = domainReplicatorProvider.get();
        domainReplicator.start(domain);
        domainReplicators.add(domainReplicator);
        replicatorMap.put(domain.getDomain(), domainReplicator);
    }

    public List<DomainReplicator> getDomainReplicators() {
        return Collections.unmodifiableList(domainReplicators);
    }

}
