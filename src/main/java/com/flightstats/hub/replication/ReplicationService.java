package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
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

    private final ReplicationDao replicationDao;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final CuratorLock curatorLock;
    private final Replicator replicator;
    private final ReplicationValidator replicationValidator;

    @Inject
    public ReplicationService(ReplicationDao replicationDao, ChannelService channelService, ChannelUtils channelUtils,
                              CuratorLock curatorLock, Replicator replicator,
                              ReplicationValidator replicationValidator) {
        this.replicationDao = replicationDao;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
        this.curatorLock = curatorLock;
        this.replicator = replicator;
        this.replicationValidator = replicationValidator;
    }

    public void create(final ReplicationDomain domain) {
        replicationValidator.validateDomain(domain);

        curatorLock.runWithLock(new Lockable() {
            @Override
            public void runWithLock() throws Exception {
                replicationDao.upsert(domain);
            }
        }, LOCK_PATH, 1, TimeUnit.MINUTES);
        notifyWatchers();
    }

    private void notifyWatchers() {
        replicator.notifyWatchers();
    }

    public Optional<ReplicationDomain> get(String domain) {
        return replicationDao.get(domain);
    }

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

    public Collection<ReplicationDomain> getDomains(boolean refreshCache) {
        return replicationDao.getDomains(refreshCache);
    }

    public ReplicationBean getReplicationBean() {
        return new ReplicationBean(getDomains(false), getStatus());
    }

    private Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (V1ChannelReplicator channelReplicator : domainReplicator.getChannels()) {
                statuses.add(getChannel(channelReplicator));
            }
        }
        return statuses;
    }

    public ReplicationStatus getStatus(String channel) {
        for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (V1ChannelReplicator channelReplicator : domainReplicator.getChannels()) {
                if (channelReplicator.getChannel().getName().equals(channel)) {
                    return getChannel(channelReplicator);
                }
            }
        }
        return null;
    }

    private ReplicationStatus getChannel(V1ChannelReplicator channelReplicator) {
        ReplicationStatus status = new ReplicationStatus();
        Channel channel = channelReplicator.getChannel();
        status.setChannel(channel);
        Optional<Long> sourceLatest = channelUtils.getLatestSequence(channel.getUrl());
        if (channelReplicator.isValid() && sourceLatest.isPresent()) {
            status.setConnected(channelReplicator.isConnected());
            Optional<ContentKey> lastUpdatedKey = channelService.getLatest(channel.getName(), true);
            if (lastUpdatedKey.isPresent()) {
                status.setReplicationLatest(lastUpdatedKey.get().toUrl());
            }
            status.setSourceLatest(sourceLatest.get().toString());
        } else if (!sourceLatest.isPresent()) {
            status.setMessage("source channel not present");
        } else {
            status.setMessage(channelReplicator.getMessage());
        }
        return status;
    }
}
