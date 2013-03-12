package com.flightstats.datahub.service.eventing;

import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class JettyWebSocketServlet extends WebSocketServlet {

	private final SubscriptionDispatcher subscriptionDispatcher;

	@Inject
	public JettyWebSocketServlet(SubscriptionDispatcher subscriptionDispatcher) {
		this.subscriptionDispatcher = subscriptionDispatcher;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		WebSocketCreator creator = new CustomWebSocketCreator(subscriptionDispatcher);
		//		factory.register(DataHubWebSocket.class);	//to let jetty create our instances for us
		factory.setCreator(creator);
	}

	public static class CustomWebSocketCreator implements WebSocketCreator {
		private final SubscriptionDispatcher subscriptionDispatcher;

		public CustomWebSocketCreator(SubscriptionDispatcher subscriptionDispatcher) {
			this.subscriptionDispatcher = subscriptionDispatcher;
		}

		@Override
		public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
			return new DataHubWebSocket(subscriptionDispatcher);
		}
	}
}
