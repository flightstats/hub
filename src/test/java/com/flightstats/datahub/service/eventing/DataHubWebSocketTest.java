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
    private Session session;
    private URI requestUri;
    private SubscriptionRoster subscriptionRoster;
    private WebSocketChannelNameExtractor channelNameExtractor;

    @Before
	public void setup() {
        requestUri = URI.create("http://path.to.site.com:999/channel/" + CHANNEL_NAME + "/ws");
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("superawesome.com", 999);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        subscriptionRoster = mock( SubscriptionRoster.class );
        channelNameExtractor = mock( WebSocketChannelNameExtractor.class );
        session = mock(Session.class);
        UpgradeRequest upgradeRequest = mock(UpgradeRequest.class);

		when(session.getRemoteAddress()).thenReturn(remoteAddress);
		when(session.getRemote()).thenReturn(remoteEndpoint);
		when(session.getUpgradeRequest()).thenReturn(upgradeRequest);
	    when(channelNameExtractor.extractChannelName(requestUri)).thenReturn(CHANNEL_NAME);
		when(upgradeRequest.getRequestURI()).thenReturn(requestUri);
	}

	@Test
	public void testOnConnect() throws Exception {
		DataHubWebSocket testClass = new DataHubWebSocket( subscriptionRoster, channelNameExtractor );
		testClass.onConnect(session);

        verify( subscriptionRoster ).subscribe( eq( CHANNEL_NAME ), any( JettyWebSocketEndpointSender.class ) );
        verify( channelNameExtractor ).extractChannelName( requestUri );
		verify(session).getRemoteAddress();
	}

	@Test
	public void testOnDisconnect() throws Exception {

		Runnable disconnectCallback = mock( Runnable.class );
		DataHubWebSocket testClass = new DataHubWebSocket( subscriptionRoster, channelNameExtractor, disconnectCallback );
		testClass.onConnect(session);
		testClass.onDisconnect(99, "spoon");
		verify(disconnectCallback).run();
		verify(subscriptionRoster).unsubscribe( eq(CHANNEL_NAME), any( JettyWebSocketEndpointSender.class ) );
	}
}
