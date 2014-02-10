package com.flightstats.hub.websocket;

public interface Consumer<T> {

	public void apply(T t);
}
