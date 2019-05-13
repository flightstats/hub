package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.rest.Linked;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

/**
 * This resource represents operations on tags.
 */
@Path("/tag")
public class TagResource {

    private final ChannelService channelService;
    private final LinkBuilder linkBuilder;

    @Context
    private UriInfo uriInfo;

    @Inject
    public TagResource(ChannelService channelService, LinkBuilder linkBuilder) {
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Map<String, URI> tagUriMap = new TreeMap<>();
        for (String tag : channelService.getTags()) {
            tagUriMap.put(tag, URI.create(uriInfo.getBaseUri() + "tag/" + tag));
        }
        Linked<?> result = this.linkBuilder.buildLinks(uriInfo, tagUriMap, "tags");
        return Response.ok(result).build();
    }

}
