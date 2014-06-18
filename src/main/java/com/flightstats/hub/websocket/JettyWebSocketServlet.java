package com.flightstats.hub.websocket;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.util.ChannelNameUtils;
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
	private final ChannelNameUtils channelNameUtils;
	private final ChannelService channelService;

	@Inject
	public JettyWebSocketServlet(WebSocketCreator webSocketCreator, ChannelNameUtils channelNameUtils, ChannelService channelService) {
		this.channelService = channelService;
		this.creator = webSocketCreator;
		this.channelNameUtils = channelNameUtils;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestUriString = request.getRequestURI();
		URI requestUri = URI.create(requestUriString);
		String channelName = channelNameUtils.extractFromWS(requestUri);
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
		factory.setCreator(creator);
	}
}
