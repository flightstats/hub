package com.flightstats.datahub.replication;

import com.flightstats.datahub.cluster.CuratorLock;
import com.flightstats.datahub.cluster.Lockable;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.exception.InvalidRequestException;
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

    @Inject
    public ReplicationService(DynamoReplicationDao replicationDao,
                              ChannelService channelService, ChannelUtils channelUtils,
                              CuratorLock curatorLock, CuratorFramework curator) {
        this.replicationDao = replicationDao;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
        this.curatorLock = curatorLock;
        this.curator = curator;
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
            curator.setData().forPath(Replicator.REPLICATOR_WATCHER_PATH, Longs.toByteArray(System.currentTimeMillis()));
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

    public Collection<ReplicationDomain> getConfigs() {
        return replicationDao.getConfigs();
    }

    public Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        //todo - gfm - 1/29/14 - figure out this circular reference
        /*for (Replicator.SourceReplicator sourceReplicator : replicator.getReplicators()) {
            for (String url : sourceReplicator.getSourceChannelUrls()) {
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

        }*/

        return statuses;
    }
}
