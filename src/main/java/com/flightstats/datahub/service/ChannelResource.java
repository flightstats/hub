package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.google.common.base.Strings;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents the collection of all channels in the DataHub.
 */
@Path("/channel")
public class ChannelResource {

	private final ChannelDao channelDao;
	private final ChannelHypermediaLinkBuilder linkBuilder;

	@Inject
	public ChannelResource(ChannelDao channelDao, ChannelHypermediaLinkBuilder linkBuilder) {
		this.channelDao = channelDao;
		this.linkBuilder = linkBuilder;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getChannels() {
		throw new RuntimeException("Channels metadata is not yet implemented");
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createChannel(ChannelCreationRequest channelCreationRequest) {
		String channelName = channelCreationRequest.getName();
		if (channelName == null || Strings.isNullOrEmpty(channelName.trim())) {
			return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Channel name cannot be blank\"}").build();
		}

		ChannelConfiguration channelConfiguration = channelDao.createChannel(channelName);
		URI channelUri = linkBuilder.buildChannelUri(channelConfiguration);
		return Response.created( channelUri ).entity(
			linked(channelConfiguration)
				.withLink("self", channelUri)
				.withLink("latest", linkBuilder.buildLatestUri(channelName))
				.withLink("ws", linkBuilder.buildWsLinkFor(channelName))
				.build())
			.build();
	}
}
