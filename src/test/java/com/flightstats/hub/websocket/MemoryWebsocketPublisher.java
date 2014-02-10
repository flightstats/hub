package com.flightstats.hub.websocket;

import com.flightstats.hub.model.ContentKey;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class MemoryWebsocketPublisher implements WebsocketPublisher {

    private final Map<String, MessageListener<String>> messageListenerMap = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(1000));

    @Override
    public void publish(final String channelName, final ContentKey key) {
        final MessageListener<String> listener = messageListenerMap.get(channelName);
        if (listener == null) {
            return;
        }
        //todo - gfm - 2/10/14 - messages sent to the WebSocket need to be single threaded.
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Message<String> message = new Message<>(channelName, key.keyToString(), System.currentTimeMillis(), null);
                listener.onMessage(message);
            }
        });
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
