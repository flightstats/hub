package com.flightstats.datahub.service.eventing;

public interface Consumer<T> {

	public void apply(T t);
}
