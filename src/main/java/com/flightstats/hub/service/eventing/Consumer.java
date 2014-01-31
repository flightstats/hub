package com.flightstats.hub.service.eventing;

public interface Consumer<T> {

	public void apply(T t);
}
