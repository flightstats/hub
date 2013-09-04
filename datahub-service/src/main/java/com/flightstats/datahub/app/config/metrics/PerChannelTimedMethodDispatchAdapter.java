package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;

import javax.ws.rs.ext.Provider;

/**
 * This is a jersey ResourceMethodDispatchAdapter that knows how to produce a RequestDispatcher that knows
 * what to do with @PerChannelTimed annotated resource methods.
 */
@Provider
public class PerChannelTimedMethodDispatchAdapter implements ResourceMethodDispatchAdapter {

	private final MetricRegistry registry;

	@Inject
	public PerChannelTimedMethodDispatchAdapter(MetricRegistry registry) {
		this.registry = registry;
	}

	@Override
	public ResourceMethodDispatchProvider adapt(final ResourceMethodDispatchProvider provider) {
		return new ResourceMethodDispatchProvider() {
			@Override
			public RequestDispatcher create(final AbstractResourceMethod abstractResourceMethod) {
				final RequestDispatcher delegate = provider.create(abstractResourceMethod);
				return new PerChannelTimedRequestDispatcher(registry, abstractResourceMethod, delegate);
			}
		};
	}

}
