package com.flightstats.datahub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.replication.ChannelUtils;
import com.flightstats.datahub.replication.Replicator;
import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/replication")
public class ReplicatorResource {
    private final Replicator replicator;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private static ObjectMapper mapper = new ObjectMapper();

    @Inject
    public ReplicatorResource(Replicator replicator, ChannelService channelService, ChannelUtils channelUtils) {
        this.replicator = replicator;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() throws Exception {
        ArrayNode rootNode = mapper.createArrayNode();
        for (Replicator.SourceReplicator sourceReplicator : replicator.getReplicators()) {
            for (String channelUrl : sourceReplicator.getSourceChannelUrls()) {
                ObjectNode channelNode = rootNode.addObject();
                channelNode.put("source", channelUrl);
                String name = ChannelNameExtractor.extractFromChannelUrl(channelUrl);
                Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(name);
                if (lastUpdatedKey.isPresent()) {
                    //todo - gfm - 1/23/14 - this will need to change to support TimeSeries
                    SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
                    channelNode.put("migrateLatest", contentKey.getSequence());
                }
                Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
                if (latestSequence.isPresent()) {
                    channelNode.put("sourcesLatest", latestSequence.get());
                }
            }
        }
        return Response.ok(rootNode).build();
    }


}
