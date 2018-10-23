package com.flightstats.hub.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.metrics.ActiveTraces;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

@Provider
public class TracesFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(TracesFilter.class);

    private final ObjectMapper mapper;

    @Inject
    TracesFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        logger.trace("response {} {} {}", request.getMethod(), requestUri, response.getStatus());
        boolean trace = Boolean.valueOf(request.getUriInfo().getQueryParameters().getFirst("trace"));
        if (trace) {
            Object entity = response.getEntity();
            if (entity == null) {
                logger.info("trace == null");
            } else {
                if (mapper.canSerialize(entity.getClass())) {
                    entity = mapper.writeValueAsString(entity);
                }
                logger.info("trace {} {}", entity.getClass(), entity);
            }
        }
        Thread thread = Thread.currentThread();
        if (!ActiveTraces.end(trace, response.getStatus())) {
            logger.debug("unable to end trace for {}", requestUri);
        }
        thread.setName(StringUtils.substringBefore(thread.getName(), "|"));
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        String ipAddress = request.getHeaders().getFirst("X-Forwarded-For");
        logger.trace("incoming {} {} {}", requestUri, request.getMethod(), ipAddress);
        Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "|" + request.getMethod() + "|" + requestUri);
        ActiveTraces.start(requestUri, request.getMethod(), ipAddress);
    }
}
