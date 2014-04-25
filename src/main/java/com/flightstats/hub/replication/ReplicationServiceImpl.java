package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.flightstats.hub.model.exception.InvalidRequestException;
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
public class ReplicationServiceImpl implements ReplicationService {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationServiceImpl.class);
    private static final String LOCK_PATH = "/ReplicationService/";

    private final ReplicationDao replicationDao;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final CuratorLock curatorLock;
    private final CuratorFramework curator;
    private final Replicator replicator;

    @Inject
    public ReplicationServiceImpl(ReplicationDao replicationDao,
                                  ChannelService channelService, ChannelUtils channelUtils,
                                  CuratorLock curatorLock, CuratorFramework curator, Replicator replicator) {
        this.replicationDao = replicationDao;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
        this.curatorLock = curatorLock;
        this.curator = curator;
        this.replicator = replicator;
    }

    @Override
    public void create(final String domain, final ReplicationDomain config) {
        if (!config.isValid()) {
            throw new InvalidRequestException("Invalid request. Either includeExcept or excludeExcept must be populated.");
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

    @Override
    public Optional<ReplicationDomain> get(String domain) {
        return replicationDao.get(domain);
    }

    @Override
    public boolean delete(final String domain) {
        Optional<ReplicationDomain> optionalDomain = replicationDao.get(domain);
        if (!optionalDomain.isPresent()) {
            return false;
        }
        curatorLock.runWithLock(new Lockable() {
            @Override
            public void runWithLock() throws Exception {
                replicationDao.delete(domain);
            }
        }, LOCK_PATH, 1, TimeUnit.MINUTES);
        notifyWatchers();
        return true;
    }

    @Override
    public Collection<ReplicationDomain> getDomains(boolean refreshCache) {
        return replicationDao.getDomains(refreshCache);
    }

    @Override
    public ReplicationBean getReplicationBean() {
        return new ReplicationBean(getDomains(false), getStatus());
    }

    private Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (ChannelReplicator channelReplicator : domainReplicator.getChannels()) {
                ReplicationStatus status = new ReplicationStatus();
                statuses.add(status);
                Channel channel = channelReplicator.getChannel();
                status.setChannel(channel);
                if (channelReplicator.isValid()) {
                    status.setConnected( channelReplicator.isConnected());
                    Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel.getName());
                    if (lastUpdatedKey.isPresent()) {
                        SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
                        status.setReplicationLatest(contentKey.getSequence());
                    }
                    Optional<Long> latestSequence = channelUtils.getLatestSequence(channel.getUrl());
                    if (latestSequence.isPresent()) {
                        status.setSourceLatest(latestSequence.get());
                    }
                } else {
                    status.setMessage(channelReplicator.getMessage());
                }
            }
        }

        return statuses;
    }
}
