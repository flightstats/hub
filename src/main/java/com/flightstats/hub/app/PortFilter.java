package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

@Provider
@PreMatching
public class PortFilter implements ContainerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(PortFilter.class);

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        UriInfo uriInfo = request.getUriInfo();
        if (uriInfo.getBaseUri().getPort() == 80) {
            URI baseUri = UriBuilder.fromUri(uriInfo.getBaseUri()).port(-1).build();
            URI requestUri = UriBuilder.fromUri(uriInfo.getRequestUri()).port(-1).build();
            request.setRequestUri(baseUri, requestUri);
            logger.trace("ignoring default port with {} and {}", baseUri, requestUri);
        }
    }
}
