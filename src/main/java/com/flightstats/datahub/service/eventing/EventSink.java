package com.flightstats.datahub.service.eventing;

public interface EventSink<T> {

	public void sink(T t);
}
