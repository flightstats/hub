package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.channel.ChannelLinkBuilder;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;

/**
 *
 */
public class ReplicationService {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationService.class);
    public static final String REPLICATED = "replicated";

    @Inject
    private ChannelService channelService;
    @Inject
    private ChannelUtils channelUtils;
    @Inject
    private Replicator replicator;

    public Iterable<ChannelConfiguration> getReplicatingChannels() {
        return channelService.getChannels(REPLICATED);
    }

    public void getStatus(String channel, ObjectNode node, UriInfo uriInfo) {
        ChannelConfiguration config = channelService.getChannelConfiguration(channel);
        if (config == null || !config.isReplicating()) {
            return;
        }
        node.put("name", config.getName());
        String localUri = ChannelLinkBuilder.buildChannelUri(config, uriInfo).toString();
        node.put("localHref", localUri);
        node.put("replicationSource", config.getReplicationSource());
        V1ChannelReplicator v1ChannelReplicator = replicator.getChannelReplicator(channel);
        if (v1ChannelReplicator == null) {
            node.put("message", "replicationSource channel not found");
            return;
        }
        Optional<Long> sourceLatest = channelUtils.getLatestSequence(config.getReplicationSource());
        if (v1ChannelReplicator.isValid() && sourceLatest.isPresent()) {
            Optional<ContentKey> lastUpdatedKey = channelService.getLatest(channel, true, false);
            if (lastUpdatedKey.isPresent()) {
                node.put("localLatest", localUri + "/" + lastUpdatedKey.get().toUrl());
            }
            node.put("replicationSourceLatest", config.getReplicationSource() + "/" + sourceLatest.get().toString());
        } else if (!sourceLatest.isPresent()) {
            node.put("message", "replicationSource latest not found");
        } else {
            node.put("message", v1ChannelReplicator.getMessage());
        }
    }

}
