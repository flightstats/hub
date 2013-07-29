package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

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
		getTopicForChannel(channelName).publish(keyRenderer.keyToString(result.getKey()));
	}

	public void addListener(String channelName, MessageListener<String> messageListener) {
		getTopicForChannel(channelName).addMessageListener(messageListener);
	}

	public void removeListener(String channelName, MessageListener<String> messageListener) {
		getTopicForChannel(channelName).removeMessageListener(messageListener);
	}

	private ITopic<String> getTopicForChannel(String channelName) {
		return hazelcast.getTopic("ws:" + channelName);
	}
}
