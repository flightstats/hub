package com.flightstats.hub.websocket;

import com.flightstats.hub.service.ChannelLinkBuilder;
import com.flightstats.hub.util.ChannelNameExtractor;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.mockito.Mockito.*;

public class HubWebSocketTest {

	public static final String CHANNEL_NAME = "tumbleweed";
    private Session session;
    private URI requestUri;
    private WebsocketSubscribers websocketSubscribers;
    private ChannelNameExtractor channelNameExtractor;
	private ChannelLinkBuilder linkBuilder;

    @Before
	public void setup() {
        requestUri = URI.create("http://path.to.site.com:999/channel/" + CHANNEL_NAME + "/ws");
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("superawesome.com", 999);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        websocketSubscribers = mock( WebsocketSubscribers.class );
        channelNameExtractor = mock( ChannelNameExtractor.class );
        session = mock(Session.class);
		linkBuilder = mock(ChannelLinkBuilder.class);
        UpgradeRequest upgradeRequest = mock(UpgradeRequest.class);

		when(session.getRemoteAddress()).thenReturn(remoteAddress);
		when(session.getRemote()).thenReturn(remoteEndpoint);
		when(session.getUpgradeRequest()).thenReturn(upgradeRequest);
	    when(channelNameExtractor.extractFromWS(requestUri)).thenReturn(CHANNEL_NAME);
		when(upgradeRequest.getRequestURI()).thenReturn(requestUri);
	}

	@Test
	public void testOnConnect() throws Exception {
		HubWebSocket testClass = new HubWebSocket(websocketSubscribers, channelNameExtractor, linkBuilder);
		testClass.onConnect(session);

        verify(websocketSubscribers).subscribe( eq( CHANNEL_NAME ), any( WebsocketConsumer.class ) );
        verify( channelNameExtractor ).extractFromWS(requestUri);
		verify(session).getRemoteAddress();
	}

	@Test
	public void testOnDisconnect() throws Exception {

		Runnable disconnectCallback = mock( Runnable.class );
		HubWebSocket testClass = new HubWebSocket(websocketSubscribers, channelNameExtractor, linkBuilder, disconnectCallback );
		testClass.onConnect(session);
		testClass.onDisconnect(99, "spoon");
		verify(disconnectCallback).run();
		verify(websocketSubscribers).unsubscribe( eq(CHANNEL_NAME), any( WebsocketConsumer.class ) );
	}
}
