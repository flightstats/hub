package com.flightstats.hub.metrics;

import com.flightstats.hub.model.Traces;
import com.flightstats.hub.model.TracesImpl;
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
        Traces traces = new TracesImpl();
        traces.add(request.getRequestUri());
        threadLocal.set(traces);
        ActiveTraces.add(traces);
        logger.trace("setting {}", traces);
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Traces traces = threadLocal.get();
        if (null == traces) {
            logger.warn("no Traces found!");
        } else {
            logger.trace("removing {}", traces.getId());
            ActiveTraces.remove(traces);
            threadLocal.remove();
        }
        return response;
    }
}
