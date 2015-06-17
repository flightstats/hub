package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
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
import java.util.HashMap;
import java.util.Map;

/**
 * This resource represents operations on tags.
 */
@Path("/tag")
public class TagResource {

    private final ChannelService channelService;
    private final UriInfo uriInfo;

    @Inject
    public TagResource(ChannelService channelService, UriInfo uriInfo) {
        this.channelService = channelService;
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Iterable<String> tags = channelService.getTags();
        Map<String, URI> tagUriMap = new HashMap<>();
        for (String tag : tags) {
            tagUriMap.put(tag, URI.create(uriInfo.getBaseUri() + "tag/" + tag));
        }
        Linked<?> result = LinkBuilder.buildLinks(uriInfo, tagUriMap, "tags");
        return Response.ok(result).build();
    }

    @GET
    @Path("/{tag}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagLinks(@PathParam("tag") String tag) {
        Iterable<ChannelConfig> channels = channelService.getChannels(tag);
        Map<String, URI> mappedUris = new HashMap<>();
        for (ChannelConfig channelConfig : channels) {
            String channelName = channelConfig.getName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        Linked<?> result = LinkBuilder.buildLinks(mappedUris, "channels", builder -> {
            String uri = uriInfo.getRequestUri().toString();
            builder.withLink("self", uriInfo.getRequestUri())
                    //.withLink("latest", uri + "/latest")
                    //.withLink("earliest", uri + "/earliest")
                    //.withLink("status", uri + "/status")
                    .withLink("time", uri + "/time");

        });
        return Response.ok(result).build();
    }

}
