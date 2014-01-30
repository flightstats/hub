package com.flightstats.datahub.replication;

import com.flightstats.datahub.cluster.CuratorLock;
import com.flightstats.datahub.cluster.Lockable;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ReplicationService {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationService.class);
    private static final String LOCK_PATH = "/ReplicationService/";

    private final DynamoReplicationDao replicationDao;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final CuratorLock curatorLock;
    private final CuratorFramework curator;
    private final Replicator replicator;

    @Inject
    public ReplicationService(DynamoReplicationDao replicationDao,
                              ChannelService channelService, ChannelUtils channelUtils,
                              CuratorLock curatorLock, CuratorFramework curator, Replicator replicator) {
        this.replicationDao = replicationDao;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
        this.curatorLock = curatorLock;
        this.curator = curator;
        this.replicator = replicator;
    }

    public void create(final String domain, final ReplicationDomain config) {
        if (!config.isValid()) {
            throw new InvalidRequestException("only one of includeExcept and excludeExcept can be populated");
        }
        curatorLock.runWithLock(new Lockable() {
            @Override
            public void runWithLock() throws Exception {
                config.setDomain(domain);
                replicationDao.upsert(config);
            }
        }, LOCK_PATH, 1, TimeUnit.MINUTES);
        notifyWatchers();
    }

    private void notifyWatchers() {
        try {
            curator.setData().forPath(ReplicatorImpl.REPLICATOR_WATCHER_PATH, Longs.toByteArray(System.currentTimeMillis()));
        } catch (Exception e) {
            logger.warn("unable to set watcher path", e);
        }
    }

    public Optional<ReplicationDomain> get(String domain) {
        return replicationDao.get(domain);
    }

    public void delete(final String domain) {
        curatorLock.runWithLock(new Lockable() {
            @Override
            public void runWithLock() throws Exception {
                replicationDao.delete(domain);
            }
        }, LOCK_PATH, 1, TimeUnit.MINUTES);
        notifyWatchers();
    }

    public Collection<ReplicationDomain> getDomains() {
        return replicationDao.getDomains();
    }

    public ReplicationBean getReplicationBean() {
        return new ReplicationBean(getDomains(), getStatus());
    }

    private Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (String url : domainReplicator.getSourceChannelUrls()) {
                ReplicationStatus status = new ReplicationStatus();
                status.setUrl(url);
                statuses.add(status);
                String name = ChannelNameExtractor.extractFromChannelUrl(url);
                Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(name);
                if (lastUpdatedKey.isPresent()) {
                    //todo - gfm - 1/23/14 - this will need to change to support TimeSeries
                    SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
                    status.setReplicationLatest(contentKey.getSequence());
                }
                Optional<Long> latestSequence = channelUtils.getLatestSequence(url);
                if (latestSequence.isPresent()) {
                    status.setSourceLatest(latestSequence.get());
                }
            }

        }

        return statuses;
    }
}
