package com.flightstats.hub.websocket;

import com.hazelcast.core.MessageListener;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
public class WebsocketSubscribersTest {

    private final String channelName = "4chan";
    private Consumer<String> consumer;
    private WebsocketPublisher publisher;
    private WebsocketSubscribers websocketSubscribers;
    private ArgumentCaptor<MessageListener> messageListenerCaptor;

    //todo - gfm - 10/30/14 -
    /*@Before
    public void setUp() throws Exception {

        consumer = mock(Consumer.class);
        publisher = mock(WebsocketPublisherImpl.class);
        websocketSubscribers = new WebsocketSubscribers(publisher);
        messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
    }

    @Test
	public void testSubscribe() throws Exception {
        String key = "54321";

		Message message = mock(Message.class);
        when(publisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn("54321");

        when(message.getMessageObject()).thenReturn(key);

		websocketSubscribers.subscribe(channelName, consumer);

		verify(publisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(key);
	}

	@Test
	public void testUnsubscribe() throws Exception {

        String id = "12345";
        when(publisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn(id);

		websocketSubscribers.subscribe(channelName, consumer);
		websocketSubscribers.unsubscribe(channelName, consumer);

		verify(publisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		verify(publisher).unsubscribe(channelName, id);
		assertEquals(0, websocketSubscribers.getTotalSubscriberCount());
	}*/
}
