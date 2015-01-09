package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * An adapter for dispatching metrics to Hosted Graphite.
 */
@Singleton
public class HubMethodTimingAdapterProvider
        implements Provider<HubInstrumentedResourceMethodDispatchAdapter> {
    HubInstrumentedResourceMethodDispatchAdapter adapter;

    @Inject
    public HubMethodTimingAdapterProvider( HostedGraphiteSender graphiteSender) {
        adapter = new HubInstrumentedResourceMethodDispatchAdapter(graphiteSender);
    }

    @Override
    public HubInstrumentedResourceMethodDispatchAdapter get() {
        return adapter;
    }
}

