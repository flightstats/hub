package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.service.eventing.SingleProcessSubscriptionRoster;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.flightstats.datahub.service.eventing.WebSocketEventSubscription;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HazelcastSubscriptionRoster implements SubscriptionRoster {

	private final static Logger logger = LoggerFactory.getLogger(HazelcastSubscriptionRoster.class);
	private final SingleProcessSubscriptionRoster delegate;
	private final HazelcastInstance hazelcast;
	private final Map<WebSocketEventSubscription, MessageListener<URI>> subscriptionToMessageListener = new ConcurrentHashMap<>();

	@Inject
	//Note: The delegate needs to be the specific concrete type to get Guice to inject properly.  Wishing there was a more elegant solution to this.
	public HazelcastSubscriptionRoster(SingleProcessSubscriptionRoster delegate, HazelcastInstance hazelcast) {
		this.delegate = delegate;
		this.hazelcast = hazelcast;
	}

	@Override
	public WebSocketEventSubscription subscribe(final String channelName, Consumer<URI> consumer) {
		MessageListener<URI> messageListener = addTopicListenerForChannel(channelName, consumer);
		WebSocketEventSubscription subscription = delegate.subscribe(channelName, consumer);
		subscriptionToMessageListener.put(subscription, messageListener);
		return subscription;
	}

	private MessageListener<URI> addTopicListenerForChannel(final String channelName, final Consumer<URI> consumer) {
		ITopic<URI> topic = hazelcast.getTopic("ws:" + channelName);
		logger.info("Adding new message listener for websocket hazelcast queue for channel " + channelName);
		MessageListener<URI> messageListener = new MessageListener<URI>() {
			@Override
			public void onMessage(Message<URI> message) {
				// When we get something from the topic, pass it along to the delegate consumer
				URI uri = message.getMessageObject();
				consumer.apply(uri);
			}
		};
		topic.addMessageListener(messageListener);
		return messageListener;
	}

	@Override
	public void unsubscribe(String channelName, WebSocketEventSubscription subscription) {
		delegate.unsubscribe(channelName, subscription);
		ITopic<URI> topic = hazelcast.getTopic("ws:" + channelName);
		MessageListener<URI> messageListener = subscriptionToMessageListener.remove(subscription);
		logger.info("Removing message listener for websocket hazelcast queue for channel " + channelName);
		topic.removeMessageListener(messageListener);
	}

	@Override
	public Optional<WebSocketEventSubscription> findSubscriptionForConsumer(String channelName, Consumer<URI> consumer) {
		return delegate.findSubscriptionForConsumer(channelName, consumer);
	}

	@Override
	public Integer getTotalSubscriberCount() {
		return delegate.getTotalSubscriberCount();
	}

	@Override
	public Collection<WebSocketEventSubscription> getSubscribers(String channelName) {
		return delegate.getSubscribers(channelName);
	}

}
