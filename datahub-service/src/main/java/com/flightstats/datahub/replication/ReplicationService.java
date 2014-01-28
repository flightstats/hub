package com.flightstats.datahub.replication;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class ReplicationService {

    //todo - gfm - 1/27/14 - do all the modifying methods need global locks?

    private Replicator replicator;
    private final DynamoReplicationDao replicationDao;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;

    @Inject
    public ReplicationService(Replicator replicator, DynamoReplicationDao replicationDao,
                              ChannelService channelService, ChannelUtils channelUtils) {
        this.replicator = replicator;
        this.replicationDao = replicationDao;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
    }

    public void create(String domain, ReplicationConfig config) {
        if (!config.getIncludeExcept().isEmpty() && !config.getExcludeExcept().isEmpty()) {
            throw new InvalidRequestException("only one of includeExcept and excludeExcept can be populated");
        }
        config.setDomain(domain);
        replicationDao.upsert(config);
        //todo - gfm - 1/27/14 - notify via ZK
    }

    public Optional<ReplicationConfig> get(String domain) {
        return replicationDao.get(domain);
    }

    public void delete(String domain) {
        replicationDao.delete(domain);
        //todo - gfm - 1/27/14 - notify via ZK
    }

    public Collection<ReplicationStatus> getStatus() {
        ArrayList<ReplicationStatus> statuses = Lists.newArrayList();
        for (Replicator.SourceReplicator sourceReplicator : replicator.getReplicators()) {
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

        }

        return statuses;
    }
}
