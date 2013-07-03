package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Hazelcast subscriber that listens for on a specific channel. There is one HazelcastSubscriber for each websocket client.
 */
public class HazelcastSubscriber implements MessageListener<URI> {

	private final static Logger logger = LoggerFactory.getLogger(HazelcastSubscriber.class);
	static final short BUFFER = (short) 1000;

	private final Consumer<URI> consumer;
	private final DataHubKeyRenderer keyRenderer;
	private short nextExpected = -1;
	private final Map<Short,Message<URI>> futureMessages = new ConcurrentHashMap<>();
	private final Object mutex = new Object();


	public HazelcastSubscriber(Consumer<URI> consumer, DataHubKeyRenderer keyRenderer) {
		this.consumer = consumer;
		this.keyRenderer = keyRenderer;
	}

	@Override
	public void onMessage(Message<URI> message) {
		if (message == null) return;

		synchronized (mutex) {
			short messageSequence = getKeyFromUri(message.getMessageObject()).getSequence();
			if (handleFirstMessage(message, messageSequence)) return;

			switch (compareExpected(nextExpected, messageSequence, BUFFER)) {
				case 0: handleExpectedMessage(message); break;
				case 1: handleFutureMessage(message, messageSequence); break;
				case -1: handleOldMessage(message); break;
			}
		}
	}

	private boolean handleFirstMessage(Message<URI> message, short messageSequence) {
		if (nextExpected == -1) {
			nextExpected = getNextExpected(messageSequence);
			consumer.apply(message.getMessageObject());
			return true;
		}
		return false;
	}

	private void handleExpectedMessage(Message<URI> message) {
		nextExpected = getNextExpected(nextExpected);
		consumer.apply(message.getMessageObject());
		onMessage(futureMessages.remove(nextExpected));
	}

	private void handleFutureMessage(Message<URI> message, short messageSequence) {
		futureMessages.put(messageSequence, message);
	}

	private void handleOldMessage(Message<URI> message) {
		logger.error("Ignoring old message to avoid out of sequence send to subscriber: " + message.getMessageObject());
	}

	private short getNextExpected(short current) {
		return (short) (current == Short.MAX_VALUE ? 0 : current + 1);
	}

	/**
	 * Check if the message sequence is newer, older, or equal to the expected value. Rollover makes this
	 * complicated since a "future" message sequence could be either higher than the expected or lower if expected is near
	 * the rollover point, which is where the buffer comes in. A message is "newer" only if it's in the buffer range.
	 *
	 * @return 0 if equal, 1 if received is "future", -1 if received is in the past.
	 */
	static int compareExpected(short expected, short received, short buffer) {
		if (expected == received) return 0;
		boolean isFutureBeforeRollover = (received > expected) && (received < Math.min(expected + buffer, Short.MAX_VALUE));
		boolean isFutureAfterRollover = received < (expected + buffer - Short.MAX_VALUE);
		return isFutureBeforeRollover || isFutureAfterRollover ? 1 : -1;
	}

	private DataHubKey getKeyFromUri(URI uri) {
		String[] foo = uri.getPath().split("/");
		return keyRenderer.fromString(foo[foo.length - 1]);
	}
}
