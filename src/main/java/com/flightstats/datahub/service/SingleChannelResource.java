package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.service.eventing.SubscriptionDispatcher;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the DataHub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {

	private final ChannelDao channelDao;
	private final SubscriptionDispatcher subscriptionDispatcher;
	private final ChannelHypermediaLinkBuilder linkBuilder;

	@Inject
	public SingleChannelResource(ChannelDao channelDao, SubscriptionDispatcher subscriptionDispatcher, ChannelHypermediaLinkBuilder linkBuilder) {
		this.channelDao = channelDao;
		this.linkBuilder = linkBuilder;
		this.subscriptionDispatcher = subscriptionDispatcher;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Linked<ChannelConfiguration> getChannelMetadata(@PathParam("channelName") String channelName) {
		if (!channelDao.channelExists(channelName)) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		ChannelConfiguration config = channelDao.getChannelConfiguration(channelName);
		return linked(config)
				.withLink("self", linkBuilder.buildChannelUri(config))
				.withLink("latest", linkBuilder.buildLatestUri(config))
				.build();
	}


	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertValue(@HeaderParam("Content-Type") String contentType, @PathParam(
			"channelName") String channelName, byte[] data) {
		if (!channelDao.channelExists(channelName)) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		ValueInsertionResult insertionResult = channelDao.insert(channelName, contentType, data);

		URI payloadUri = linkBuilder.buildItemUri(insertionResult.getKey());
		subscriptionDispatcher.dispatch(channelName, payloadUri);

		Linked<ValueInsertionResult> linkedResult = linked(insertionResult)
				.withLink("channel", linkBuilder.buildChannelUri(channelName))
				.withLink("self", payloadUri)
				.build();

		Response.ResponseBuilder builder = Response.status(Response.Status.OK);
		builder.entity(linkedResult);
		builder.location(payloadUri);
		return builder.build();
	}

}
