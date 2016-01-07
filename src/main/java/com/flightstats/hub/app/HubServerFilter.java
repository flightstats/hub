package com.flightstats.hub.app;

import com.google.inject.Inject;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;


/**
 * Add Hub Server header to all requests
 */
public class HubServerFilter implements ContainerResponseFilter {

    @Inject
    HubVersion hubVersion;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().putSingle("Server", "Hub/" + hubVersion.getVersion());
    }
}