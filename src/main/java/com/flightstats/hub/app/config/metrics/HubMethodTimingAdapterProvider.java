package com.flightstats.hub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * An adapter for dispatching metrics to Graphite.
 */
@Singleton
public class HubMethodTimingAdapterProvider
        implements Provider<HubInstrumentedResourceMethodDispatchAdapter>
{
    HubInstrumentedResourceMethodDispatchAdapter adapter;

    @Inject
    public HubMethodTimingAdapterProvider(MetricRegistry registry, HostedGraphiteSender graphiteSender)
    {
        adapter = new HubInstrumentedResourceMethodDispatchAdapter( registry, graphiteSender );
    }

    @Override
    public HubInstrumentedResourceMethodDispatchAdapter get()
    {
        return adapter;
    }
}

