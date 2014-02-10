package com.flightstats.hub.websocket;

import com.flightstats.hub.model.ContentKey;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class MemoryWebsocketPublisher implements WebsocketPublisher {

    private Map<String, MessageListener<String>> messageListenerMap = new ConcurrentHashMap<>();

    @Override
    public void publish(final String channelName, final ContentKey key) {
        final MessageListener<String> listener = messageListenerMap.get(channelName);
        if (listener == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message<String> message = new Message<>(channelName, key.keyToString(), System.currentTimeMillis(), null);
                listener.onMessage(message);
            }
        }).start();
    }

    @Override
    public String subscribe(String channelName, MessageListener<String> messageListener) {
        messageListenerMap.put(channelName, messageListener);
        return channelName;
    }

    @Override
    public void unsubscribe(String channelName, String registrationId) {
        messageListenerMap.remove(channelName);
    }
}
