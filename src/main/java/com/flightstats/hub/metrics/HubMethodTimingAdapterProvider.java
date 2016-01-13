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
    public HubMethodTimingAdapterProvider(MetricsSender graphiteSender) {
        //todo - gfm - 1/6/16 -
        //adapter = new HubInstrumentedResourceMethodDispatchAdapter(graphiteSender);
        adapter = new HubInstrumentedResourceMethodDispatchAdapter();
    }

    @Override
    public HubInstrumentedResourceMethodDispatchAdapter get() {
        return adapter;
    }
}

