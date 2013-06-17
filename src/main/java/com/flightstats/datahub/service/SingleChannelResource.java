package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.MetadataResponse;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Callable;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the DataHub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {

	private final ChannelDao channelDao;
	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final ChannelLockExecutor channelLockExecutor;
	private final HazelcastInstance hazelcast;

	@Inject
	public SingleChannelResource(ChannelDao channelDao, ChannelHypermediaLinkBuilder linkBuilder, ChannelLockExecutor channelLockExecutor, HazelcastInstance hazelcast) {
		this.channelDao = channelDao;
		this.linkBuilder = linkBuilder;
		this.channelLockExecutor = channelLockExecutor;
		this.hazelcast = hazelcast;
	}

	@GET
	@Timed
	@PerChannelTimed(operationName = "metadata", channelNamePathParameter = "channelName")
	@Produces(MediaType.APPLICATION_JSON)
	public Linked<MetadataResponse> getChannelMetadata(@PathParam("channelName") String channelName) {
		if (!channelDao.channelExists(channelName)) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}

		ChannelConfiguration config = channelDao.getChannelConfiguration(channelName);
		Date lastUpdateDate = getLastUpdateDate(channelName);
		MetadataResponse response = new MetadataResponse(config, lastUpdateDate);
		return linked(response)
				.withLink("self", linkBuilder.buildChannelUri(config))
				.withLink("latest", linkBuilder.buildLatestUri())
				.withLink("ws", linkBuilder.buildWsLinkFor())
				.build();
	}

	private Date getLastUpdateDate(String channelName) {
		Optional<DataHubKey> latestId = channelDao.findLatestId(channelName);
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

		Callable<ValueInsertionResult> task = new WriteAndDispatch(channelName, contentType, data);
		ValueInsertionResult insertionResult = channelLockExecutor.execute(channelName, task);

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

	private class WriteAndDispatch implements Callable<ValueInsertionResult> {
		private final String channelName;
		private final String contentType;
		private final byte[] data;

		public WriteAndDispatch(String channelName, String contentType, byte[] data) {
			this.channelName = channelName;
			this.contentType = contentType;
			this.data = data;
		}

		@Override
		public ValueInsertionResult call() throws Exception {
			ValueInsertionResult result = channelDao.insert(channelName, contentType, data);
			URI payloadUri = linkBuilder.buildItemUri(result.getKey());
			ITopic<URI> topic = hazelcast.getTopic("ws:" + channelName);
			topic.publish(payloadUri);
			return result;
		}
	}
}
