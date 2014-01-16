package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.eventing.Consumer;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subscriber that listens for on a specific channel.
 * There is one SequenceSubscriber for each websocket client.
 * This ensures that all messages are received in sequential order.
 */
public class SequenceSubscriber implements MessageListener<String> {

    private final static Logger logger = LoggerFactory.getLogger(SequenceSubscriber.class);

    private final Consumer<String> consumer;
    private final Map<Long, String> futureMessages = new ConcurrentHashMap<>();
    private long nextExpected = -1;


    public SequenceSubscriber(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessage(Message<String> message) {
        String stringKey = message.getMessageObject();
        long messageSequence = getKeyFromUri(stringKey).getSequence();

        if (isFirst()) {
            consumer.apply(stringKey);
            nextExpected = getNextExpected(messageSequence);
        } else if (isFuture(messageSequence)) {
            futureMessages.put(messageSequence, stringKey);    //buffer it up
        } else if (isOld(messageSequence)) {
            logger.error("Ignoring old message(expected=" + nextExpected + ", ignored=" + messageSequence + "):" + message.getMessageObject());
        } else {
            futureMessages.put(messageSequence, stringKey);
            dispatchBufferedInOrder();
        }
    }

    private void dispatchBufferedInOrder() {
        String nextUri;
        while ((nextUri = futureMessages.remove(nextExpected)) != null) {
            consumer.apply(nextUri);
            nextExpected = getNextExpected(nextExpected);
        }
    }

    private boolean isFirst() {
        return nextExpected == -1;
    }

    private boolean isFuture(long received) {
        return (received > nextExpected);
    }

    private boolean isOld(long messageSequence) {
        return !(isFirst() || isExpected(messageSequence) || isFuture(messageSequence));
    }

    private boolean isExpected(long messageSequence) {
        return nextExpected == messageSequence;
    }

    private long getNextExpected(long current) {
        return current + 1;
    }

    private SequenceContentKey getKeyFromUri(String stringKey) {
        return (SequenceContentKey) SequenceContentKey.fromString(stringKey).get();
    }
}
