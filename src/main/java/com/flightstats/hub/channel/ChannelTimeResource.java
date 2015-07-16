package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/channel/{channel: .*}/time")
public class ChannelTimeResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelTimeResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UriInfo uriInfo;

    @Inject
    private ChannelService channelService;

    @Inject
    public ChannelTimeResource(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault(@PathParam("channel") String channel) {
        ChannelConfig channelConfig = channelService.getChannelConfig(channel);
        if (null == channelConfig) {
            return Response.status(404).build();
        }
        return TimeLinkUtil.getDefault(uriInfo);
    }

    @Path("/second")
    @GET
    public Response getSecond(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getSecond(stable, uriInfo);
    }

    @Path("/minute")
    @GET
    public Response getMinute(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getMinute(stable, uriInfo);
    }

    @Path("/hour")
    @GET
    public Response getHour(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getHour(stable, uriInfo);
    }

    @Path("/day")
    @GET
    public Response getDay(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getDay(stable, uriInfo);
    }

}
