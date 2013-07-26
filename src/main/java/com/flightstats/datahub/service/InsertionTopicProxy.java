package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

//todo figure out a better name and write unit tests!
public class InsertionTopicProxy {
	private final HazelcastInstance hazelcast;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public InsertionTopicProxy(HazelcastInstance hazelcast, DataHubKeyRenderer keyRenderer) {
		this.hazelcast = hazelcast;
		this.keyRenderer = keyRenderer;
	}

	public void publish(String channelName, ValueInsertionResult result) {
		ITopic<String> topic = hazelcast.getTopic("ws:" + channelName);
		topic.publish(keyRenderer.keyToString(result.getKey()));
	}

}
