package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This resource represents the collection of all channels in the DataHub.
 */
@Path("/channel")
public class ChannelResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelResource.class);

	private final ChannelService channelService;
	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final UriInfo uriInfo;

	@Inject
	public ChannelResource(ChannelService channelService, ChannelHypermediaLinkBuilder linkBuilder, UriInfo uriInfo) {
		this.channelService = channelService;
		this.linkBuilder = linkBuilder;
		this.uriInfo = uriInfo;
    }

	@GET
	@Timed
    @ExceptionMetered
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChannels() {
		Iterable<ChannelConfiguration> channels = channelService.getChannels();
		Map<String, URI> mappedChannels = new HashMap<>();
		for (ChannelConfiguration channelConfiguration : channels) {
			String channelName = channelConfiguration.getName();
			mappedChannels.put(channelName, linkBuilder.buildChannelUri(channelName, uriInfo));
		}

		Linked.Builder<?> responseBuilder = Linked.justLinks();
		responseBuilder.withLink("self", uriInfo.getRequestUri());

		List<HalLink> channelLinks = new ArrayList<>(mappedChannels.size());
		for (Map.Entry<String, URI> entry : mappedChannels.entrySet()) {
			HalLink link = new HalLink(entry.getKey(), entry.getValue());
			channelLinks.add(link);
		}
		responseBuilder.withLinks("channels", channelLinks);
		Linked<?> result = responseBuilder.build();
		return Response.ok(result).build();
	}

	@POST
	@Timed
    @ExceptionMetered
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createChannel(ChannelConfiguration channelConfiguration) throws InvalidRequestException, AlreadyExistsException {
        channelService.createChannel(channelConfiguration);
		URI channelUri = linkBuilder.buildChannelUri(channelConfiguration, uriInfo);
		return Response.created(channelUri).entity(
			linkBuilder.buildChannelLinks(channelConfiguration, channelUri))
			.build();
	}
}
