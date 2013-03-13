package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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
	public Linked<ChannelConfiguration> createChannel(ChannelCreationRequest channelCreationRequest) {
		ChannelConfiguration channelConfiguration = channelDao.createChannel(channelCreationRequest.getName());
		return linked(channelConfiguration)
				.withLink("self", linkBuilder.buildChannelUri(channelConfiguration))
				.withLink("latest", linkBuilder.buildLatestUri(channelConfiguration))
				.build();
	}

}
