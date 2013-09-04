package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.cluster.HazelcastSubscriber;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionRoster {

	private final static Logger logger = LoggerFactory.getLogger(SubscriptionRoster.class);
	private final ChannelInsertionPublisher channelInsertionPublisher;
	private final DataHubKeyRenderer keyRenderer;
	private final ConcurrentHashMap<ChannelConsumer, MessageListener<String>> consumerToMessageListener = new ConcurrentHashMap<>();

	@Inject
	public SubscriptionRoster(ChannelInsertionPublisher channelInsertionPublisher, DataHubKeyRenderer keyRenderer) {
		this.keyRenderer = keyRenderer;
		this.channelInsertionPublisher = channelInsertionPublisher;
	}

	public void subscribe(final String channelName, Consumer<String> consumer) {
		MessageListener<String> messageListener = addTopicListenerForChannel( channelName, consumer );
		consumerToMessageListener.put( new ChannelConsumer( channelName, consumer ), messageListener );
	}

	private MessageListener<String> addTopicListenerForChannel(final String channelName, final Consumer<String> consumer) {
		logger.info("Adding new message listener for websocket hazelcast queue for channel " + channelName);
		MessageListener<String> messageListener = new HazelcastSubscriber(consumer, keyRenderer);
		channelInsertionPublisher.subscribe(channelName, messageListener);
		return messageListener;
	}

	public void unsubscribe(String channelName, Consumer<String> subscription) {
		MessageListener<String> messageListener = consumerToMessageListener.remove(new ChannelConsumer(channelName, subscription));
		logger.info("Removing message listener for websocket hazelcast queue for channel " + channelName);
		channelInsertionPublisher.unsubscribe(channelName, messageListener);
	}

	public int getTotalSubscriberCount() {
		return consumerToMessageListener.size();
	}

	private static class ChannelConsumer {
		private final String channelName;
		private final Consumer<String> consumer;

		ChannelConsumer( String channelName, Consumer<String> consumer ) {

			this.channelName = channelName;
			this.consumer = consumer;
		}

		public boolean equals( Object o ) {
			if ( this == o ) return true;
			if ( !(o instanceof ChannelConsumer) ) return false;

			ChannelConsumer that = (ChannelConsumer) o;

			if ( !channelName.equals( that.channelName ) ) return false;
			if ( !consumer.equals( that.consumer ) ) return false;

			return true;
		}

		public int hashCode() {
			int result = channelName.hashCode();
			result = 31 * result + consumer.hashCode();
			return result;
		}
	}
}
