package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubVersion;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * Add Hub Server header to all requests
 */
@Provider
public class HubServerFilter implements ContainerResponseFilter {

    private final HubVersion hubVersion;

    @Inject
    public HubServerFilter(HubVersion hubVersion) {
        this.hubVersion = hubVersion;
    }


    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Server", "Hub/" + hubVersion.getVersion());
    }
}