package com.flightstats.hub.app;

import com.flightstats.hub.config.properties.LocalHostProperties;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class HubResponseFilter implements ContainerResponseFilter {

    private final LocalHostProperties localHostProperties;

    @Inject
    public HubResponseFilter(LocalHostProperties localHostProperties){
        this.localHostProperties = localHostProperties;

    }
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Hub-Node", localHostProperties.getNameWithPort());
    }

}
