package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

//todo figure out a better name and write unit tests!
public class InsertionTopicProxy {
	private final HazelcastInstance hazelcast;
	private final ChannelHypermediaLinkBuilder linkBuilder;

	@Inject
	public InsertionTopicProxy(HazelcastInstance hazelcast, ChannelHypermediaLinkBuilder linkBuilder) {
		this.hazelcast = hazelcast;
		this.linkBuilder = linkBuilder;
	}

	public void publish(String channelName, ValueInsertionResult result, UriInfo uriInfo) {
		URI payloadUri = linkBuilder.buildItemUri(result.getKey(), uriInfo);
		ITopic<URI> topic = hazelcast.getTopic("ws:" + channelName);
		topic.publish(payloadUri);
	}

}
