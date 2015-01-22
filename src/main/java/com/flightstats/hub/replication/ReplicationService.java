package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class ReplicationService {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationService.class);
    private static final String LOCK_PATH = "/ReplicationService/";

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final CuratorLock curatorLock;
    private final Replicator replicator;

    @Inject
    public ReplicationService(ChannelService channelService, ChannelUtils channelUtils,
                              CuratorLock curatorLock, Replicator replicator) {
        this.channelService = channelService;
        this.channelUtils = channelUtils;
        this.curatorLock = curatorLock;
        this.replicator = replicator;
    }

    private Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        //todo - gfm - 1/22/15 -
        /*for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (V1ChannelReplicator channelReplicator : domainReplicator.getChannels()) {
                statuses.add(getChannel(channelReplicator));
            }
        }*/
        return statuses;
    }

    public ReplicationStatus getStatus(String channel) {
        //todo - gfm - 1/22/15 -
        /*for (DomainReplicator domainReplicator : replicator.getDomainReplicators()) {
            for (V1ChannelReplicator channelReplicator : domainReplicator.getChannels()) {
                if (channelReplicator.getChannel().getName().equals(channel)) {
                    return getChannel(channelReplicator);
                }
            }
        }*/
        return null;
    }

    private ReplicationStatus getChannel(V1ChannelReplicator channelReplicator) {
        //todo - gfm - 1/22/15 -
        ReplicationStatus status = new ReplicationStatus();
        ChannelConfiguration channel = channelReplicator.getChannel();
        /*status.setChannel(channel);
        Optional<Long> sourceLatest = channelUtils.getLatestSequence(channel.getUrl());
        if (channelReplicator.isValid() && sourceLatest.isPresent()) {
            status.setConnected(channelReplicator.isConnected());
            Optional<ContentKey> lastUpdatedKey = channelService.getLatest(channel.getName(), true, false);
            if (lastUpdatedKey.isPresent()) {
                status.setReplicationLatest(lastUpdatedKey.get().toUrl());
            }
            status.setSourceLatest(sourceLatest.get().toString());
        } else if (!sourceLatest.isPresent()) {
            status.setMessage("source channel not present");
        } else {
            status.setMessage(channelReplicator.getMessage());
        }*/
        return status;
    }
}
