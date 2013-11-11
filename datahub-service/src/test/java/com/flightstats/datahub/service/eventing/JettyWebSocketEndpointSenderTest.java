package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelHypermediaLinkBuilder;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class JettyWebSocketEndpointSenderTest {

	@Test
	public void testSink() throws Exception {
		String address = "here";
		URI requestUri = URI.create("http://dorkbot.org");
		URI itemUri = URI.create("http://flightstats.com/datahub/channel/itemKey");
		String itemKey = "itemKey";
		DataHubKey dataHubKey = new DataHubKey((short) 5);

		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);
		DataHubKeyRenderer keyRenderer = mock(DataHubKeyRenderer.class);

		when(keyRenderer.fromString(itemKey)).thenReturn(Optional.of(dataHubKey));
		when(linkBuilder.buildItemUri(dataHubKey, requestUri)).thenReturn(itemUri);

		JettyWebSocketEndpointSender testClass = new JettyWebSocketEndpointSender(address, remoteEndpoint, linkBuilder, keyRenderer, requestUri);
		testClass.apply(itemKey);

		verify(remoteEndpoint).sendString(itemUri.toString());
	}

	@Test(expected = RuntimeException.class)
	public void testIOExceptionWrapping() throws Exception {
		String address = "here";
		URI requestUri = URI.create("http://dorkbot.org");
		URI itemUri = URI.create("http://flightstats.com/datahub/channel/itemKey");
		String itemKey = "itemKey";
		DataHubKey dataHubKey = new DataHubKey((short) 5);

		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);
		DataHubKeyRenderer keyRenderer = mock(DataHubKeyRenderer.class);

		when(keyRenderer.fromString(itemKey)).thenReturn(Optional.of(dataHubKey));
		when(linkBuilder.buildItemUri(dataHubKey, requestUri)).thenReturn(itemUri);

		doThrow(new IOException("Error!  Error!")).when(remoteEndpoint).sendString(anyString());
		JettyWebSocketEndpointSender testClass = new JettyWebSocketEndpointSender(address, remoteEndpoint, linkBuilder, keyRenderer, requestUri);

		testClass.apply(itemKey);
	}

}
