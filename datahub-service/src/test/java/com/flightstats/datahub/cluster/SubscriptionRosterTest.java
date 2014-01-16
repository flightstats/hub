package com.flightstats.datahub.cluster;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class SubscriptionRosterTest {

    private final String channelName = "4chan";
    private Consumer<String> consumer;
    private ChannelInsertionPublisher publisher;
    private ChannelService channelService;
    private ChannelConfiguration configuration;
    private SubscriptionRoster subscriptionRoster;
    private ArgumentCaptor<MessageListener> messageListenerCaptor;

    @Before
    public void setUp() throws Exception {

        consumer = mock(Consumer.class);
        publisher = mock(ChannelInsertionPublisher.class);
        channelService = mock(ChannelService.class);
        configuration = ChannelConfiguration.builder().withName(channelName).build();
        when(channelService.getChannelConfiguration(channelName)).thenReturn(configuration);
        subscriptionRoster = new SubscriptionRoster(publisher, channelService);
        messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
    }

    @Test
	public void testSubscribe() throws Exception {
        String key = "54321";

		Message message = mock(Message.class);
        when(publisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn("54321");

        when(message.getMessageObject()).thenReturn(key);

		subscriptionRoster.subscribe(channelName, consumer);

		verify(publisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(key);
	}

	@Test
	public void testUnsubscribe() throws Exception {

        String id = "12345";
        when(publisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn(id);

		subscriptionRoster.subscribe(channelName, consumer);
		subscriptionRoster.unsubscribe(channelName, consumer);

		verify(publisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		verify(publisher).unsubscribe(channelName, id);
		assertEquals(0, subscriptionRoster.getTotalSubscriberCount());
	}
}
