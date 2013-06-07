package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
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
	private final CreateChannelValidator createChannelValidator;

	@Inject
	public ChannelResource(ChannelDao channelDao, ChannelHypermediaLinkBuilder linkBuilder, CreateChannelValidator createChannelValidator ) {
		this.channelDao = channelDao;
		this.linkBuilder = linkBuilder;
		this.createChannelValidator = createChannelValidator;
	}

	@GET
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	public String getChannels() {
		throw new RuntimeException("Channels metadata is not yet implemented");
	}

	@POST
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createChannel(ChannelCreationRequest channelCreationRequest) throws InvalidRequestException, AlreadyExistsException {
		String channelName = channelCreationRequest.getName();
		createChannelValidator.validate( channelCreationRequest );

		ChannelConfiguration channelConfiguration = channelDao.createChannel(channelName);
		URI channelUri = linkBuilder.buildChannelUri(channelConfiguration);
		return Response.created(channelUri).entity(
				linked(channelConfiguration)
						.withLink("self", channelUri)
						.withLink("latest", linkBuilder.buildLatestUri(channelName))
						.withLink("ws", linkBuilder.buildWsLinkFor(channelName))
						.build())
					   .build();
	}
}
