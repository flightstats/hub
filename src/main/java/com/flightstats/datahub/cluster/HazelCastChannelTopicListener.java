package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.eventing.Consumer;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.net.URI;

/**
 * A Hazelcast topic listener for messages on a specific channel.
 */
public class HazelCastChannelTopicListener implements MessageListener<URI> {

	final Consumer<URI> consumer;

	public HazelCastChannelTopicListener(Consumer<URI> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void onMessage(Message<URI> message) {
		// When we get something from the topic, pass it along to the delegate consumer
		URI uri = message.getMessageObject();
		consumer.apply(uri);
	}
}
