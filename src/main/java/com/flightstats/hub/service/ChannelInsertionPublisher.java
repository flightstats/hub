package com.flightstats.hub.service;

import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class ChannelInsertionPublisher {
	private final HazelcastInstance hazelcast;
    private final MetricsTimer metricsTimer;

    @Inject
	public ChannelInsertionPublisher(HazelcastInstance hazelcast, MetricsTimer metricsTimer) {
		this.hazelcast = hazelcast;
        this.metricsTimer = metricsTimer;
    }

	public void publish(final String channelName, final ContentKey key) {
        metricsTimer.time("hazelcast.publish", new TimedCallback<Object>() {
            @Override
            public Object call() {
                getTopicForChannel(channelName).publish(key.keyToString());
                return null;
            }
        });
	}

	public String subscribe(String channelName, MessageListener<String> messageListener) {
        return getTopicForChannel(channelName).addMessageListener(messageListener);
    }

	public void unsubscribe(String channelName, String registrationId) {
		getTopicForChannel(channelName).removeMessageListener(registrationId);
	}

	private ITopic<String> getTopicForChannel(String channelName) {
		return hazelcast.getTopic("ws:" + channelName);
	}
}
