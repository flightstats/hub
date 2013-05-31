package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.dao.ChannelDao;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

public class JettyWebSocketServlet extends WebSocketServlet {

	private final static Logger logger = LoggerFactory.getLogger(JettyWebSocketServlet.class);
	private final WebSocketCreator creator;
	private final WebSocketChannelNameExtractor channelNameExtractor;
	private final ChannelDao channelDao;

	@Inject
	public JettyWebSocketServlet(SubscriptionRoster subscriptions, WebSocketChannelNameExtractor channelNameExtractor, ChannelDao channelDao) {
		this.channelDao = channelDao;
		this.creator = new CustomWebSocketCreator(subscriptions, channelNameExtractor);
		this.channelNameExtractor = channelNameExtractor;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestUriString = request.getRequestURI();
		URI requestUri = URI.create(requestUriString);
		String channelName = channelNameExtractor.extractChannelName(requestUri);
		if (!channelDao.channelExists(channelName)) {
			logger.warn("No such channel '" + channelName + "', refusing websocket upgrade request.");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		invokeSuper(request, response);
	}

	@VisibleForTesting
	protected void invokeSuper(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.service(request, response);
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		//		factory.register(DataHubWebSocket.class);	//to let jetty create our instances for us
		factory.setCreator(creator);
	}

	public static class CustomWebSocketCreator implements WebSocketCreator {
		private final SubscriptionRoster subscriptions;
		private final WebSocketChannelNameExtractor channelNameExtractor;

		public CustomWebSocketCreator(SubscriptionRoster subscriptions, WebSocketChannelNameExtractor channelNameExtractor) {
			this.subscriptions = subscriptions;
			this.channelNameExtractor = channelNameExtractor;
		}

		@Override
		public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
			return new DataHubWebSocket(subscriptions, channelNameExtractor);
		}
	}
}
