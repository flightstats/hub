package com.flightstats.hub.app;

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Add Hub Server header to all requests
 */
public class HubServerFilter implements ContainerResponseFilter {

    @Inject
    HubVersion hubVersion;

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        response.getHttpHeaders().putSingle("Server", "Hub/" + hubVersion.getVersion());
        return response;
    }
}