package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.eventing.Consumer;
import com.hazelcast.core.Message;
import org.junit.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Mockito.*;

public class SequenceSubscriberTest {

    @Test
    public void testOneMessageBasic() throws URISyntaxException {
        // GIVEN
        ContentKey key = new SequenceContentKey(1000);
        Consumer<String> consumer = mock(Consumer.class);
        SequenceSubscriber testClass = new SequenceSubscriber(consumer);

        // WHEN
        testClass.onMessage(new Message<>("foo", key.keyToString(), 0L, null));

        // THEN
        verify(consumer).apply(key.keyToString());
    }

    @Test
    public void testTwoMessageInOrder() throws URISyntaxException {
        // GIVEN
        ContentKey key_1 = new SequenceContentKey(1000);
        ContentKey key_2 = new SequenceContentKey(1001);
        String stringKey1 = key_1.keyToString();
        String stringKey2 = key_2.keyToString();
        Consumer<String> consumer = mock(Consumer.class);
        InOrder messageOrder = inOrder(consumer);
        SequenceSubscriber testClass = new SequenceSubscriber(consumer);

        // WHEN
        testClass.onMessage(new Message<>("foo", stringKey1, 0L, null));
        testClass.onMessage(new Message<>("foo", stringKey2, 0L, null));

        // THEN
        messageOrder.verify(consumer).apply(stringKey1);
        messageOrder.verify(consumer).apply(stringKey2);
    }

    @Test
    public void testTwoMessageOutOfOrder() throws URISyntaxException {
        // GIVEN
        ContentKey key_1 = new SequenceContentKey(1000);
        ContentKey key_2 = new SequenceContentKey(1001);
        String stringKey1 = key_1.keyToString();
        String stringKey2 = key_2.keyToString();
        Consumer<String> consumer = mock(Consumer.class);
        SequenceSubscriber testClass = new SequenceSubscriber(consumer);

        // WHEN
        testClass.onMessage(new Message<>("foo", stringKey2, 0L, null));
        testClass.onMessage(new Message<>("foo", stringKey1, 0L, null));

        // THEN
        verify(consumer, times(1)).apply(stringKey2);
        verify(consumer, times(0)).apply(stringKey1);
    }

    @Test
    public void testMessagesFromTheFutureAndPastAndRollover() throws URISyntaxException {
        // GIVEN
        ContentKey key_1 = new SequenceContentKey((Short.MAX_VALUE - 2));
        ContentKey key_2 = new SequenceContentKey((Short.MAX_VALUE - 1));
        ContentKey key_3 = new SequenceContentKey(Short.MAX_VALUE);
        ContentKey key_4 = new SequenceContentKey(Short.MAX_VALUE + 1);
        ContentKey key_5 = new SequenceContentKey(Short.MAX_VALUE + 2);
        String key1 = key_1.keyToString();
        URI uri_1 = new URI("http://mysystem:7898/channel/mychan/" + key1);
        String key2 = key_2.keyToString();
        URI uri_2 = new URI("http://mysystem:7898/channel/mychan/" + key2);
        String key3 = key_3.keyToString();
        URI uri_3 = new URI("http://mysystem:7898/channel/mychan/" + key3);
        String key4 = key_4.keyToString();
        URI uri_4 = new URI("http://mysystem:7898/channel/mychan/" + key4);
        String key5 = key_5.keyToString();
        URI uri_5 = new URI("http://mysystem:7898/channel/mychan/" + key5);
        Consumer<String> consumer = mock(Consumer.class);
        InOrder messageOrder = inOrder(consumer);
        SequenceSubscriber testClass = new SequenceSubscriber(consumer);

        // WHEN
        testClass.onMessage(new Message<>("foo", key2, 0L, null)); // first message
        testClass.onMessage(new Message<>("foo", key1, 0L, null)); // old and thrown out, lower sequence than uri_2
        testClass.onMessage(new Message<>("foo", key4, 0L, null)); // future
        testClass.onMessage(new Message<>("foo", key5, 0L, null)); // future
        testClass.onMessage(new Message<>("foo", key3, 0L, null)); // expected, then futures should get handled

        // THEN
        messageOrder.verify(consumer).apply(key2);
        messageOrder.verify(consumer).apply(key3);
        messageOrder.verify(consumer).apply(key4);
        messageOrder.verify(consumer).apply(key5);
        verify(consumer, times(4)).apply(anyString());
    }
}
