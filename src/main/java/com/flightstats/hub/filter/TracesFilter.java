package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.ActiveTraces;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Provider
public class TracesFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(TracesFilter.class);

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        logger.trace("response {} {} {}", request.getMethod(), requestUri, response.getStatus());
        Thread thread = Thread.currentThread();
        thread.setName(StringUtils.substringBefore(thread.getName(), "|"));
        if (!ActiveTraces.end()) {
            logger.debug("unable to end trace for {}", requestUri);
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        logger.trace("incoming {} {}", request.getMethod(), requestUri);
        Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "|" + requestUri);
        MultivaluedMap<String, String> headers = request.getHeaders();
        Set<String> keySet = headers.keySet();
        logger.info("incoming header keys {}", keySet);

        ActiveTraces.start(request.getMethod(), requestUri);
    }
}
