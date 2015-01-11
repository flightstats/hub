package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This resource represents operations on tags.
 */
@Path("/tag")
public class TagResource {

    private final ChannelService channelService;
    private final ChannelLinkBuilder linkBuilder;
    private final UriInfo uriInfo;

    @Inject
    public TagResource(ChannelService channelService, ChannelLinkBuilder linkBuilder, UriInfo uriInfo) {
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
        this.uriInfo = uriInfo;
    }

    @GET
    @EventTimed(name = "tags.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Iterable<String> tags = channelService.getTags();
        Map<String, URI> tagUriMap = new HashMap<>();
        for (String tag : tags) {
            tagUriMap.put(tag, linkBuilder.buildTagUri(tag, uriInfo));
        }

        Linked.Builder<?> responseBuilder = Linked.justLinks();
        responseBuilder.withLink("self", uriInfo.getRequestUri());

        List<HalLink> channelLinks = new ArrayList<>(tagUriMap.size());
        for (Map.Entry<String, URI> entry : tagUriMap.entrySet()) {
            HalLink link = new HalLink(entry.getKey(), entry.getValue());
            channelLinks.add(link);
        }
        responseBuilder.withLinks("tags", channelLinks);
        Linked<?> result = responseBuilder.build();
        return Response.ok(result).build();
    }

    @GET
    @Path("/{tag}")
    @EventTimed(name = "tag.ALL.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels(@PathParam("tag") String tag) {
        Iterable<ChannelConfiguration> channels = channelService.getChannels(tag);
        Linked<?> result = linkBuilder.build(channels, uriInfo);
        return Response.ok(result).build();
    }


}
