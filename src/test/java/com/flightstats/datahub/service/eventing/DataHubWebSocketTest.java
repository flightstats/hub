package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

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
		EventSink<URI> expectedSink = new JettyWebsocketEndpointSender(remoteAddress.toString(), remoteEndpoint);
		SubscriptionDispatcher dispatcher = mock(SubscriptionDispatcher.class);

		DataHubWebSocket testClass = new DataHubWebSocket(dispatcher);

		testClass.onConnect(session);

		verify(dispatcher).subscribe(CHANNEL_NAME, expectedSink);
	}

	@Test
	public void testOnDisconnect() throws Exception {
		EventSink<URI> expectedSink = new JettyWebsocketEndpointSender(remoteAddress.toString(), remoteEndpoint);
		SubscriptionDispatcher dispatcher = mock(SubscriptionDispatcher.class);

		DataHubWebSocket testClass = new DataHubWebSocket(dispatcher);

		testClass.onDisconnect(session, 99, "spoon");

		verify(dispatcher).unsubscribe(CHANNEL_NAME, expectedSink);
	}

}
