package com.flightstats.hub.metrics;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracesFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(TracesFilter.class);

    private ThreadLocal<Traces> threadLocal = new ThreadLocal();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "|" + request.getRequestUri());
        ActiveTraces.start(request.getMethod(), request.getRequestUri());
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Thread thread = Thread.currentThread();
        thread.setName(StringUtils.substringBefore(thread.getName(), "|"));
        if (!ActiveTraces.end()) {
            logger.warn("unable to end trace for {}", request.getRequestUri());
        }
        return response;
    }
}
