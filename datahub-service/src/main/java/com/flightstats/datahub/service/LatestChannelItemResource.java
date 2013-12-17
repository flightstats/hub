package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
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
	private final ChannelDao channelDao;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public LatestChannelItemResource(UriInfo uriInfo, ChannelDao channelDao, DataHubKeyRenderer keyRenderer) {
		this.uriInfo = uriInfo;
		this.channelDao = channelDao;
		this.keyRenderer = keyRenderer;
	}

	@GET
    @PerChannelTimed(operationName = "latest", channelNamePathParameter = "channelName")
	@Timed
    @ExceptionMetered
	public Response getLatest(@PathParam("channelName") String channelName) {
		Optional<DataHubKey> latestId = channelDao.findLastUpdatedKey(channelName);
		if (!latestId.isPresent()) {
            return Response.status(NOT_FOUND).build();
		}
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);

		String channelUri = uriInfo.getRequestUri().toString().replaceFirst("/latest$", "");
		DataHubKey keyOfLatestItem = latestId.get();
		URI uri = URI.create(channelUri + "/" + keyRenderer.keyToString(keyOfLatestItem));
		builder.location(uri);
		return builder.build();
	}

}
