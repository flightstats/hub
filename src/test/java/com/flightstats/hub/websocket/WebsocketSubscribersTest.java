package com.flightstats.hub.websocket;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class WebsocketSubscribersTest {

    private final String channelName = "4chan";
    private Consumer<String> consumer;
    private WebsocketPublisher publisher;
    private ChannelService channelService;
    private ChannelConfiguration configuration;
    private WebsocketSubscribers websocketSubscribers;
    private ArgumentCaptor<MessageListener> messageListenerCaptor;

    @Before
    public void setUp() throws Exception {

        consumer = mock(Consumer.class);
        publisher = mock(WebsocketPublisherImpl.class);
        channelService = mock(ChannelService.class);
        configuration = ChannelConfiguration.builder().withName(channelName).build();
        when(channelService.getChannelConfiguration(channelName)).thenReturn(configuration);
        websocketSubscribers = new WebsocketSubscribers(publisher, channelService);
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
	}
}
