package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class JettyWebsocketEndpointSenderTest {

	@Test
	public void testSink() throws Exception {
		String address = "here";
		URI uri = URI.create("http://dorkbot.org");
		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);

		JettyWebsocketEndpointSender testClass = new JettyWebsocketEndpointSender(address, remoteEndpoint);
		testClass.apply(uri);

		verify(remoteEndpoint).sendString(uri.toString());
	}

	@Test(expected = RuntimeException.class)
	public void testIOExceptionWrapping() throws Exception {
		String address = "here";
		URI uri = URI.create("http://dorkbot.org");
		RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);

		doThrow(new IOException("Error!  Error!")).when(remoteEndpoint).sendString(anyString());
		JettyWebsocketEndpointSender testClass = new JettyWebsocketEndpointSender(address, remoteEndpoint);

		testClass.apply(uri);
	}

}
