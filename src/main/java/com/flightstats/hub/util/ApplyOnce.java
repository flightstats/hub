package com.flightstats.hub.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A function that will only execute a delegate function once.
 * On execution, the resulting optional's isPresent() will return true.
 * All subsequent executions will return an optional that is isPresent()
 * will return false;
 */
public class ApplyOnce<F, T> implements Function<F, Optional<T>> {

	private final Function<F, T> delegate;
	private final AtomicBoolean hasRun = new AtomicBoolean(false);

	public ApplyOnce(Function<F, T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Optional<T> apply(F input) {
		boolean hasRunOnce = hasRun.getAndSet(true);
		if (!hasRunOnce) {
			return Optional.fromNullable(delegate.apply(input));
		}
		return Optional.absent();
	}
}
