package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;

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
import java.util.concurrent.TimeUnit;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents the collection of all channels in the DataHub.
 */
@Path("/channel")
public class ChannelResource {
	static final Long DEFAULT_TTL = TimeUnit.DAYS.toMillis(120);

	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final UriInfo uriInfo;
	private final DataHubService dataHubService;

	@Inject
	public ChannelResource(ChannelHypermediaLinkBuilder linkBuilder, UriInfo uriInfo, DataHubService dataHubService) {
		this.linkBuilder = linkBuilder;
		this.uriInfo = uriInfo;
		this.dataHubService = dataHubService;
	}

	@GET
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChannels() {
		Iterable<ChannelConfiguration> channels = dataHubService.getChannels();
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
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createChannel(ChannelCreationRequest channelCreationRequest) throws InvalidRequestException, AlreadyExistsException {
		String channelName = channelCreationRequest.getName().trim();
		Long ttl = channelCreationRequest.getTtl() == null ? DEFAULT_TTL : channelCreationRequest.getTtl();
		ChannelConfiguration channelConfiguration = dataHubService.createChannel(channelName, ttl);
		URI channelUri = linkBuilder.buildChannelUri(channelConfiguration, uriInfo);
		return Response.created(channelUri).entity(
				linked(channelConfiguration)
						.withLink("self", channelUri)
						.withLink("latest", linkBuilder.buildLatestUri(channelName, uriInfo))
						.withLink("ws", linkBuilder.buildWsLinkFor(channelName, uriInfo))
						.build())
					   .build();
	}
}
