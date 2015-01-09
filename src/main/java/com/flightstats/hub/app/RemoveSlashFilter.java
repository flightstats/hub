package com.flightstats.hub.app;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Redirect from trailing slash URI (eg /foo/) to correct form (eg /foo).
 * This is useful for supporting consistent uri's and to avoid checking for the
 * slashes everywhere
 */
public class RemoveSlashFilter implements ContainerRequestFilter {
    public ContainerRequest filter(ContainerRequest request) {

        final String uri = request.getRequestUri().getRawPath();

        if (uri.endsWith("/") && uri.length() > 1) {
            URI unslashed = URI.create(uri.substring(0, uri.length() - 1));
            Response response = Response.temporaryRedirect(unslashed).build();
            throw new WebApplicationException(response);
        }

        return request;
    }
}