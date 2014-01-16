package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.dao.timeIndex.TimeIndex;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.SEE_OTHER;

/**
 * This resource represents groups of items stored in the DataHub
 */
@Path("/channel/{channelName: .*}/time")
public class ChannelTimeResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelTimeResource.class);
	private final UriInfo uriInfo;
	private final ChannelService channelService;
    private final ChannelHypermediaLinkBuilder linkBuilder;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
	public ChannelTimeResource(UriInfo uriInfo, ChannelService channelService, ChannelHypermediaLinkBuilder linkBuilder) {
		this.uriInfo = uriInfo;
		this.channelService = channelService;
        this.linkBuilder = linkBuilder;
    }

    @GET
    public Response getLatest() {
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        String channelUri = uriInfo.getRequestUri().toString();
        URI uri = URI.create(channelUri + "/" + TimeIndex.getHash(new DateTime()));
        builder.location(uri);
        return builder.build();
    }

    @Path("/{datetime}")
	@GET
	@Timed(name = "all-channels.ids")
	@PerChannelTimed(operationName = "ids", channelNameParameter = "channelName")
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
	public Response getValue(@PathParam("channelName") String channelName, @PathParam("datetime") String datetime)
            throws InvalidRequestException {
        DateTime requestTime = null;
        try {
            requestTime = TimeIndex.parseHash(datetime);
        } catch (Exception e) {
            logger.warn("unable to parse " + datetime + " for channel " + channelName);
            throw new InvalidRequestException("{\"error\": \"Datetime was in the wrong format, required format is "
                    + TimeIndex.PATTERN + "\"}");
        }
        Collection<ContentKey> keys = channelService.getKeys(channelName, requestTime);

        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        ArrayNode ids = links.putArray("uris");
        URI channelUri = linkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = linkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }

        return Response.ok(root).build();
	}

}
