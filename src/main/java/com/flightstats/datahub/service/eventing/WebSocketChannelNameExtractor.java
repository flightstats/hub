package com.flightstats.datahub.service.eventing;

import java.net.URI;

public class WebSocketChannelNameExtractor {

	public static final String WEBSOCKET_URL_REGEX = "^/channel/(\\w+)/ws$";

	String extractChannelName(URI requestURI) {
		String path = requestURI.getPath();
		return path.replaceFirst(WEBSOCKET_URL_REGEX, "$1");
	}
}
