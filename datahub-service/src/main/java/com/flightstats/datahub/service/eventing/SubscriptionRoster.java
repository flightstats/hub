package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.google.inject.Inject;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionRoster {

	private final static Logger logger = LoggerFactory.getLogger(SubscriptionRoster.class);
	private final ChannelInsertionPublisher channelInsertionPublisher;
	private final ConcurrentHashMap<ChannelConsumer, String> consumerToMessageListener = new ConcurrentHashMap<>();

	@Inject
	public SubscriptionRoster(ChannelInsertionPublisher channelInsertionPublisher) {
		this.channelInsertionPublisher = channelInsertionPublisher;
	}

	public void subscribe(final String channelName, final Consumer<String> consumer) {
        logger.info("Adding new message listener for websocket hazelcast queue for channel " + channelName);
        String registrationId = channelInsertionPublisher.subscribe(channelName, new MessageListener<String>() {
            @Override
            public void onMessage(Message<String> message) {
                consumer.apply(message.getMessageObject());
            }
        });
        consumerToMessageListener.put( new ChannelConsumer( channelName, consumer ), registrationId );
	}

    public void unsubscribe(String channelName, Consumer<String> subscription) {
		String registrationId = consumerToMessageListener.remove(new ChannelConsumer(channelName, subscription));
		logger.info("Removing message listener for websocket hazelcast queue for channel " + channelName);
		channelInsertionPublisher.unsubscribe(channelName, registrationId);
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
