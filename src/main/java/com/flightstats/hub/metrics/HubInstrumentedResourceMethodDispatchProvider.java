package com.flightstats.hub.metrics;


public class HubInstrumentedResourceMethodDispatchProvider {
    //todo - gfm - 1/6/16 -
}/*implements ResourceMethodDispatchProvider {
    private static class TimedRequestDispatcher implements RequestDispatcher {
        private final RequestDispatcher underlying;
        private final MetricsSender graphiteSender;
        private final String name;

        private TimedRequestDispatcher(RequestDispatcher underlying, MetricsSender graphiteSender, String name) {
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
    private final MetricsSender graphiteSender;

    public HubInstrumentedResourceMethodDispatchProvider(ResourceMethodDispatchProvider provider, MetricsSender graphiteSender) {
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
*/