package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class HubResponseFilter implements ContainerResponseFilter {

    private final HubVersion hubVersion;

    @Inject
    HubResponseFilter(HubVersion hubVersion) {
        this.hubVersion = hubVersion;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Server", "Hub/" + hubVersion.getVersion());
        responseContext.getHeaders().add("Hub-Node", HubHost.getLocalNamePort());
    }

}
