package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.cluster.SequenceSubscriber;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.google.inject.Inject;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionRoster {

	private final static Logger logger = LoggerFactory.getLogger(SubscriptionRoster.class);
	private final ChannelInsertionPublisher channelInsertionPublisher;
    private final ChannelService channelService;
    private final ConcurrentHashMap<ChannelConsumer, String> consumerToMessageListener = new ConcurrentHashMap<>();

	@Inject
	public SubscriptionRoster(ChannelInsertionPublisher channelInsertionPublisher, ChannelService channelService) {
		this.channelInsertionPublisher = channelInsertionPublisher;
        this.channelService = channelService;
    }

	public void subscribe(final String channelName, Consumer<String> consumer) {
        ChannelConfiguration configuration = channelService.getChannelConfiguration(channelName);
        if (null == configuration) {
            //todo - gfm - 1/15/14 - do we need to support websockets before channel creation?
            configuration = ChannelConfiguration.builder()
                    .withName(channelName)
                    .withType(ChannelConfiguration.ChannelType.Sequence)
                    .build();
        }

        if (configuration.isSequence()) {
            logger.info("Adding new message listener for sequence channel " + channelName);
            MessageListener<String> messageListener = new SequenceSubscriber(consumer);
            String registrationId = channelInsertionPublisher.subscribe(channelName, messageListener);
            consumerToMessageListener.put( new ChannelConsumer( channelName, consumer ), registrationId );
        } else {
            throw new UnsupportedOperationException("TimeSeries channels do not support WebSockets");
        }

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
