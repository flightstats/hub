package com.flightstats.hub.metrics;

import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;

import javax.ws.rs.ext.Provider;

@Provider
public class HubInstrumentedResourceMethodDispatchAdapter implements ResourceMethodDispatchAdapter {
    private final MetricsSender graphiteSender;

    public HubInstrumentedResourceMethodDispatchAdapter(MetricsSender graphiteSender) {
        this.graphiteSender = graphiteSender;
    }

    @Override
    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new HubInstrumentedResourceMethodDispatchProvider(provider, graphiteSender);
    }
}
