package com.flightstats.datahub.service.eventing;

import com.google.common.base.Optional;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import static org.mockito.Mockito.*;

public class DataHubWebSocketTest {

	public static final String CHANNEL_NAME = "tumbleweed";
	private URI requestUri;
	private InetSocketAddress remoteAddress;
	private RemoteEndpoint remoteEndpoint;
	private Session session;
	private UpgradeRequest upgradeRequest;

	@Before
	public void setup() {
		requestUri = URI.create("http://path.to.site.com:999/channel/" + CHANNEL_NAME + "/ws");
		remoteAddress = InetSocketAddress.createUnresolved("superawesome.com", 999);
		remoteEndpoint = mock(RemoteEndpoint.class);
		session = mock(Session.class);
		upgradeRequest = mock(UpgradeRequest.class);

		when(session.getRemoteAddress()).thenReturn(remoteAddress);
		when(session.getRemote()).thenReturn(remoteEndpoint);
		when(session.getUpgradeRequest()).thenReturn(upgradeRequest);
		when(upgradeRequest.getRequestURI()).thenReturn(requestUri);
	}

	@Test
	public void testOnConnect() throws Exception {

		BlockingQueue<WebSocketEvent> queue = mock(BlockingQueue.class);
		WebSocketEventSubscription subscriber = mock(WebSocketEventSubscription.class);
		WebSocketEvent event = mock(WebSocketEvent.class);
		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);
		WebSocketChannelNameExtractor channelNameExtractor = mock(WebSocketChannelNameExtractor.class);

		when(queue.take()).thenReturn(event);
		when(subscriber.getQueue()).thenReturn(queue);
		when(event.isShutdown()).thenReturn(true);
		when(subscriptions.subscribe(eq(CHANNEL_NAME), any(Consumer.class))).thenReturn(subscriber);
		when(subscriptions.getSubscribers(CHANNEL_NAME)).thenReturn(Arrays.asList(subscriber));
		when(channelNameExtractor.extractChannelName(requestUri)).thenReturn(CHANNEL_NAME);

		Consumer<URI> expectedConsumer = new JettyWebSocketEndpointSender(remoteAddress.toString(), remoteEndpoint);

		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions, channelNameExtractor);

		testClass.onConnect(session);

		verify(subscriptions).subscribe(CHANNEL_NAME, expectedConsumer);
	}

	@Test
	public void testOnDisconnect() throws Exception {

		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);
		WebSocketEventSubscription websocketEventSubscription = mock(WebSocketEventSubscription.class);
		BlockingQueue<WebSocketEvent> queue = mock(BlockingQueue.class);
		WebSocketEventSubscription subscriber = new WebSocketEventSubscription(null, queue);
		WebSocketEvent event = mock(WebSocketEvent.class);
		WebSocketChannelNameExtractor channelNameExtractor = mock(WebSocketChannelNameExtractor.class);

		when(queue.take()).thenReturn(event);
		when(event.isShutdown()).thenReturn(true);
		when(subscriptions.subscribe(eq(CHANNEL_NAME), any(Consumer.class))).thenReturn(websocketEventSubscription);
		when(websocketEventSubscription.getQueue()).thenReturn(queue);
		when(subscriptions.getSubscribers(CHANNEL_NAME)).thenReturn(Arrays.asList(subscriber));
		when(subscriptions.findSubscriptionForConsumer(eq(CHANNEL_NAME), any(Consumer.class))).thenReturn(Optional.of(subscriber));
		when(channelNameExtractor.extractChannelName(requestUri)).thenReturn(CHANNEL_NAME);

		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions, channelNameExtractor);
		testClass.onConnect(session);

		WebSocketEventSubscription subscription = subscriptions.getSubscribers(CHANNEL_NAME).iterator().next();

		testClass.onDisconnect(99, "spoon");

		verify(subscriptions).unsubscribe(CHANNEL_NAME, subscription);
		verify(queue).add(WebSocketEvent.SHUTDOWN);
	}

	@Test
	public void testOnDisconnectCantFindSubscription() throws Exception {
		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);
		when(subscriptions.findSubscriptionForConsumer(anyString(), any(Consumer.class))).thenReturn(Optional.<WebSocketEventSubscription>absent());
		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions, null);
		testClass.onDisconnect(99, "spoon");
		verify(subscriptions, never()).unsubscribe(anyString(), any(WebSocketEventSubscription.class));

	}

}
