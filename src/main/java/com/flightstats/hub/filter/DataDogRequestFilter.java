package com.flightstats.hub.filter;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

/**
 * Filter class to handle intercepting requests and respones from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static StatsDClient statsd = new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"});

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        String path = requestUri.getPath();
        //statsd.incrementCounter(path);
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
//        URI requestUri = request.getUriInfo().getRequestUri();
    }
}
