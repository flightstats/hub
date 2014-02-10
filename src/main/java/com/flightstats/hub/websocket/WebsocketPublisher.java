package com.flightstats.hub.websocket;

import com.flightstats.hub.model.ContentKey;
import com.hazelcast.core.MessageListener;

/**
 *
 */
public interface WebsocketPublisher {
    void publish(String channelName, ContentKey key);

    String subscribe(String channelName, MessageListener<String> messageListener);

    void unsubscribe(String channelName, String registrationId);
}
