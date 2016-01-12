package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

@Provider
@PreMatching
public class PortFilter implements ContainerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(PortFilter.class);

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        if (requestUri.getPort() == 80) {
            URI uri = UriBuilder.fromUri(requestUri).port(-1).build();
            request.setRequestUri(uri);
            logger.trace("ignoring default port with {} instead of {}", uri, requestUri);
        }
    }
}
