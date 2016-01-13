package com.flightstats.hub.metrics;

import javax.ws.rs.ext.Provider;

@Provider
public class HubInstrumentedResourceMethodDispatchAdapter {
    //todo - gfm - 1/6/16 -
}
/*implements ResourceMethodDispatchAdapter {
    private final MetricsSender graphiteSender;

    public HubInstrumentedResourceMethodDispatchAdapter(MetricsSender graphiteSender) {
        this.graphiteSender = graphiteSender;
    }

    @Override
    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new HubInstrumentedResourceMethodDispatchProvider(provider, graphiteSender);
    }
}*/
