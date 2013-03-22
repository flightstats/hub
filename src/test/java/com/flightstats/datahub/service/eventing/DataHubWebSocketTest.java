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
		Consumer<URI> expectedConsumer = new JettyWebsocketEndpointSender(remoteAddress.toString(), remoteEndpoint);
		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);

		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions);

		testClass.onConnect(session);

		verify(subscriptions).subscribe(CHANNEL_NAME, expectedConsumer);
	}

	@Test
	public void testOnDisconnect() throws Exception {
		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);

		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions);
		testClass.onConnect(session);
		BlockingQueue<WebsocketEvent> queue = mock(BlockingQueue.class);
		WebSocketEventSubscription subscriber = new WebSocketEventSubscription(null, queue);

		when(subscriptions.getSubscribers(CHANNEL_NAME)).thenReturn(Arrays.asList(subscriber));
		when(subscriptions.findSubscriptionForConsumer(eq(CHANNEL_NAME), any(Consumer.class))).thenReturn(Optional.of(subscriber));

		WebSocketEventSubscription subscription = subscriptions.getSubscribers(CHANNEL_NAME).iterator().next();

		testClass.onDisconnect(99, "spoon");

		verify(subscriptions).unsubscribe(CHANNEL_NAME, subscription);
		verify(queue).add(WebsocketEvent.SHUTDOWN);
	}

	@Test
	public void testOnDisconnectCantFindSubscription() throws Exception {
		SubscriptionRoster subscriptions = mock(SubscriptionRoster.class);
		when(subscriptions.findSubscriptionForConsumer(anyString(), any(Consumer.class))).thenReturn(Optional.<WebSocketEventSubscription>absent());
		DataHubWebSocket testClass = new DataHubWebSocket(subscriptions);
		testClass.onDisconnect(99, "spoon");
		verify(subscriptions, never()).unsubscribe(anyString(), any(WebSocketEventSubscription.class));

	}

}
