package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.dao.ChannelService;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
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

	private static final Logger logger = LoggerFactory.getLogger(JettyWebSocketServlet.class);
	private final WebSocketCreator creator;
	private final ChannelNameExtractor channelNameExtractor;
	private final ChannelService channelService;

	@Inject
	public JettyWebSocketServlet(WebSocketCreator webSocketCreator, ChannelNameExtractor channelNameExtractor, ChannelService channelService) {
		this.channelService = channelService;
		this.creator = webSocketCreator;
		this.channelNameExtractor = channelNameExtractor;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestUriString = request.getRequestURI();
		URI requestUri = URI.create(requestUriString);
		String channelName = channelNameExtractor.extractFromWS(requestUri);
		if (!channelService.channelExists(channelName)) {
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
}
