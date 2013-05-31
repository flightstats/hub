package com.flightstats.datahub.service.eventing;

import java.net.URI;

public class WebSocketEvent {

	public final static WebSocketEvent SHUTDOWN = new WebSocketEvent(null, true);

	private final URI uri;
	private final boolean shutdown;

	public WebSocketEvent(URI uri) {
		this(uri, false);
	}

	private WebSocketEvent(URI uri, boolean shutdown) {
		this.uri = uri;
		this.shutdown = shutdown;
	}

	public URI getUri() {
		return uri;
	}

	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WebSocketEvent that = (WebSocketEvent) o;

		if (shutdown != that.shutdown) {
			return false;
		}
		if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = uri != null ? uri.hashCode() : 0;
		result = 31 * result + (shutdown ? 1 : 0);
		return result;
	}
}
