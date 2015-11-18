package com.flightstats.hub.metrics;

import com.flightstats.hub.model.Traces;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracesFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(TracesFilter.class);

    private ThreadLocal<Traces> threadLocal = new ThreadLocal();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        ActiveTraces.start(request.getRequestUri());
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        ActiveTraces.end();
        return response;
    }
}
