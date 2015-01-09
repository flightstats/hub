package com.flightstats.hub.metrics;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;

public class HubInstrumentedResourceMethodDispatchProvider implements ResourceMethodDispatchProvider {
    private static class TimedRequestDispatcher implements RequestDispatcher {
        private final RequestDispatcher underlying;
        private final HostedGraphiteSender graphiteSender;
        private final String name;

        private TimedRequestDispatcher(RequestDispatcher underlying, HostedGraphiteSender graphiteSender, String name) {
            this.underlying = underlying;
            this.graphiteSender = graphiteSender;
            this.name = name;
        }

        @Override
        public void dispatch(Object resource, HttpContext httpContext) {
            long start = System.currentTimeMillis();
            try {
                underlying.dispatch(resource, httpContext);
            } finally {
                graphiteSender.send(name, System.currentTimeMillis() - start);
            }
        }
    }

    private final ResourceMethodDispatchProvider provider;
    private final HostedGraphiteSender graphiteSender;

    public HubInstrumentedResourceMethodDispatchProvider(ResourceMethodDispatchProvider provider, HostedGraphiteSender graphiteSender) {
        this.provider = provider;
        this.graphiteSender = graphiteSender;
    }

    @Override
    public RequestDispatcher create(AbstractResourceMethod method) {
        RequestDispatcher dispatcher = provider.create(method);
        if (dispatcher == null) {
            return null;
        }

        if (method.getMethod().isAnnotationPresent(EventTimed.class)) {
            final EventTimed annotation = method.getMethod().getAnnotation(EventTimed.class);
            dispatcher = new TimedRequestDispatcher(dispatcher, graphiteSender, annotation.name());
        }

        return dispatcher;
    }

}
