package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.Test;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JettyWebSocketServletTest {

	@Test
	public void testConfigure() throws Exception {
		// This test is admittedly a tad goofy, but just checking in on the jetty APIs
		SubscriptionDispatcher dispatcher = mock(SubscriptionDispatcher.class);
		WebSocketServletFactory factory = mock(WebSocketServletFactory.class);

		JettyWebSocketServlet testClass = new JettyWebSocketServlet(dispatcher);

		testClass.configure(factory);

		verify(factory).setCreator(isA(JettyWebSocketServlet.CustomWebSocketCreator.class));
	}

}
