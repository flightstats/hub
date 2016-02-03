package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubVersion;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


/**
 * Add Hub Server header to all requests
 */
@Provider
public class HubServerFilter implements ContainerResponseFilter {

    private static final HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().putSingle("Server", "Hub/" + hubVersion.getVersion());
    }
}