package com.flightstats.datahub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.migration.ChannelUtils;
import com.flightstats.datahub.migration.Migrator;
import com.flightstats.datahub.model.ContentKey;
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
@Path("/migration")
public class MigratorResource {
    private final Migrator migrator;
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private static ObjectMapper mapper = new ObjectMapper();

    @Inject
    public MigratorResource(Migrator migrator, ChannelService channelService, ChannelUtils channelUtils) {
        this.migrator = migrator;
        this.channelService = channelService;
        this.channelUtils = channelUtils;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() throws Exception {
        return getResponse(false);
    }

    @GET
    @Path("/remote")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusRemote() throws Exception {
        return getResponse(true);
    }

    private Response getResponse(boolean includeRemote) {
        ArrayNode rootNode = mapper.createArrayNode();
        for (Migrator.SourceMigrator sourceMigrator : migrator.getMigrators()) {
            for (String channelUrl : sourceMigrator.getSourceChannelUrls()) {
                ObjectNode channelNode = rootNode.addObject();
                channelNode.put("url", channelUrl);
                String name = ChannelNameExtractor.extractFromChannelUrl(channelUrl);
                channelNode.put("name", name);
                Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(name);
                if (lastUpdatedKey.isPresent()) {
                    channelNode.put("latest", lastUpdatedKey.get().keyToString());
                }
                if (includeRemote) {
                    Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
                    if (latestSequence.isPresent()) {
                        channelNode.put("remote", latestSequence.get());
                    }
                }
            }
        }
        return Response.ok(rootNode).build();
    }


}
