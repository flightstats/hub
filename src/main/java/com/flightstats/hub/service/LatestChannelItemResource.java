package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channelName: .*}/latest")
public class LatestChannelItemResource {

	private final UriInfo uriInfo;
	private final ChannelService channelService;

	@Inject
	public LatestChannelItemResource(UriInfo uriInfo, ChannelService channelService) {
		this.uriInfo = uriInfo;
		this.channelService = channelService;
	}

	@GET
    @PerChannelTimed(operationName = "latest", channelNameParameter = "channelName")
	@Timed
    @ExceptionMetered
	public Response getLatest(@PathParam("channelName") String channelName) {
		Optional<ContentKey> latestId = channelService.findLastUpdatedKey(channelName);
		if (!latestId.isPresent()) {
            return Response.status(NOT_FOUND).build();
		}
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);

		String channelUri = uriInfo.getRequestUri().toString().replaceFirst("/latest$", "");
		ContentKey keyOfLatestItem = latestId.get();
		URI uri = URI.create(channelUri + "/" + keyOfLatestItem.keyToString());
		builder.location(uri);
		return builder.build();
	}

}
