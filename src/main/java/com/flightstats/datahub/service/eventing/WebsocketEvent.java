package com.flightstats.datahub.service.eventing;

import java.net.URI;

public class WebsocketEvent {

	public final static WebsocketEvent SHUTDOWN = new WebsocketEvent(URI.create("http://flightstats.com/shutdown"));

	private final URI uri;

	public WebsocketEvent(URI uri) {
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WebsocketEvent that = (WebsocketEvent) o;

		if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return uri != null ? uri.hashCode() : 0;
	}
}
