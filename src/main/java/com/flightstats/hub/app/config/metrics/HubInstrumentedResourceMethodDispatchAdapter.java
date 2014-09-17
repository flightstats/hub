package com.flightstats.hub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;

import javax.ws.rs.ext.Provider;

/**
 * A provider that wraps a {@link com.sun.jersey.spi.container.ResourceMethodDispatchProvider} in an
 * {@link com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchProvider}
 */
@Provider
public class HubInstrumentedResourceMethodDispatchAdapter implements ResourceMethodDispatchAdapter {
    private final MetricRegistry registry;
    private final HostedGraphiteSender graphiteSender;

    /**
     * Construct a resource method dispatch adapter using the given metrics registry name.
     *
     * @param registryName the name of a shared metric registry
     */
    public HubInstrumentedResourceMethodDispatchAdapter(String registryName, HostedGraphiteSender graphiteSender) {
        this(SharedMetricRegistries.getOrCreate(registryName), graphiteSender);
    }

    /**
     * Construct a resource method dispatch adapter using the given metrics registry.
     * <p/>
     * When using this constructor, the {@link HubInstrumentedResourceMethodDispatchAdapter}
     * should be added to a Jersey {@code ResourceConfig} as a singleton.
     *
     * @param registry a {@link MetricRegistry}
     */
    public HubInstrumentedResourceMethodDispatchAdapter(MetricRegistry registry, HostedGraphiteSender graphiteSender) {
        this.registry = registry;
        this.graphiteSender = graphiteSender;
    }


    @Override
    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new HubInstrumentedResourceMethodDispatchProvider(provider, registry, graphiteSender);
    }
}
