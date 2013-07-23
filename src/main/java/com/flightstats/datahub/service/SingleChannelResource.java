package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.MetadataResponse;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the DataHub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {

	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final DataHubService dataHubService;

	@Inject
	public SingleChannelResource(ChannelHypermediaLinkBuilder linkBuilder, DataHubService dataHubService) {
		this.linkBuilder = linkBuilder;
		this.dataHubService = dataHubService;
	}

	@GET
	@Timed
	@PerChannelTimed(operationName = "metadata", channelNamePathParameter = "channelName")
	@Produces(MediaType.APPLICATION_JSON)
	public Linked<MetadataResponse> getChannelMetadata(@PathParam("channelName") String channelName) {
		if (!dataHubService.channelExists(channelName)) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}

		ChannelConfiguration config = dataHubService.getChannelConfiguration(channelName);
		Date lastUpdateDate = getLastUpdateDate(channelName);
		MetadataResponse response = new MetadataResponse(config, lastUpdateDate);
		return linked(response)
				.withLink("self", linkBuilder.buildChannelUri(config))
				.withLink("latest", linkBuilder.buildLatestUri())
				.withLink("ws", linkBuilder.buildWsLinkFor())
				.build();
	}

	private Date getLastUpdateDate(String channelName) {
		Optional<DataHubKey> latestId = dataHubService.findLatestId(channelName);
		if (!latestId.isPresent()) {
			return null;
		}
		return latestId.get().getDate();
	}

	@POST
	@Timed(name = "all-channels.insert")
	@PerChannelTimed(operationName = "insert", channelNamePathParameter = "channelName")
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertValue(@HeaderParam("Content-Type") final String contentType, @PathParam(
			"channelName") final String channelName, final byte[] data) throws Exception {

		ValueInsertionResult insertionResult = dataHubService.insert(channelName, contentType, data);

		URI payloadUri = linkBuilder.buildItemUri(insertionResult.getKey());

		Linked<ValueInsertionResult> linkedResult = linked(insertionResult)
				.withLink("channel", linkBuilder.buildChannelUri(channelName))
				.withLink("self", payloadUri)
				.build();

		Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
		builder.entity(linkedResult);
		builder.location(payloadUri);
		return builder.build();
	}

}
