package com.flightstats.hub.websocket;

import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketPublisherImpl implements WebsocketPublisher {
    private final static Logger logger = LoggerFactory.getLogger(WebsocketPublisherImpl.class);

	private final HazelcastInstance hazelcast;
    private final MetricsTimer metricsTimer;

    @Inject
	public WebsocketPublisherImpl(HazelcastInstance hazelcast, MetricsTimer metricsTimer) {
		this.hazelcast = hazelcast;
        this.metricsTimer = metricsTimer;
    }

	@Override
    public void publish(final String channelName, final ContentKey key) {
        metricsTimer.time("hazelcast.publish", new TimedCallback<Object>() {
            @Override
            public Object call() {
                try {
                    getTopicForChannel(channelName).publish(key.keyToString());
                } catch (HazelcastInstanceNotActiveException e) {
                    logger.warn("unable to publish to hazelcast due to server shutdown {} {}", channelName, key.keyToString());
                } catch (Exception e) {
                    logger.warn("unable to publish to hazelcast " + channelName + " " + key.keyToString(), e);
                }
                return null;
            }
        });
	}

	@Override
    public String subscribe(String channelName, MessageListener<String> messageListener) {
        return getTopicForChannel(channelName).addMessageListener(messageListener);
    }

	@Override
    public void unsubscribe(String channelName, String registrationId) {
		getTopicForChannel(channelName).removeMessageListener(registrationId);
	}

	private ITopic<String> getTopicForChannel(String channelName) {
		return hazelcast.getTopic("ws:" + channelName);
	}
}
