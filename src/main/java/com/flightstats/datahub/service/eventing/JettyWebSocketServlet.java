package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class JettyWebSocketServlet extends WebSocketServlet {

	@Override
	public void configure(WebSocketServletFactory factory) {
		WebSocketCreator creator = new CustomWebSocketCreator();
		//		factory.register(DataHubWebSocket.class);	//to let jetty create our instances for us
		factory.setCreator(creator);
	}

	public static class CustomWebSocketCreator implements WebSocketCreator {
		@Override
		public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
			return new DataHubWebSocket();
		}
	}
}
