package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.ChannelLinkBuilder;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class JettyWebSocketEndpointSenderTest {

	@Test
	public void testSink() throws Exception {
		String address = "here";
		URI requestUri = URI.create("http://dorkbot.org");
		URI itemUri = URI.create("http://flightstats.com/datahub/channel/itemKey");
		String itemKey = "123456";

		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);

		JettyWebSocketEndpointSender testClass = new JettyWebSocketEndpointSender(address, remoteEndpoint, new ChannelLinkBuilder(), requestUri);
		testClass.apply(itemKey);

		verify(remoteEndpoint).sendString(requestUri.toString() + "/" + itemKey);
	}

	@Test(expected = RuntimeException.class)
	public void testIOExceptionWrapping() throws Exception {
		String address = "here";
		URI requestUri = URI.create("http://dorkbot.org");
		URI itemUri = URI.create("http://flightstats.com/datahub/channel/itemKey");
		String itemKey = "itemKey";
		ContentKey contentKey = new SequenceContentKey((short) 5000);

		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
		ChannelLinkBuilder linkBuilder = mock(ChannelLinkBuilder.class);

		when(linkBuilder.buildItemUri(contentKey, requestUri)).thenReturn(itemUri);

		doThrow(new IOException("Error!  Error!")).when(remoteEndpoint).sendString(anyString());
		JettyWebSocketEndpointSender testClass = new JettyWebSocketEndpointSender(address, remoteEndpoint, linkBuilder, requestUri);

		testClass.apply(itemKey);
	}

}
