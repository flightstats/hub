package com.flightstats.hub.app;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

/**
 * Redirect from trailing slash URI (eg /foo/) to correct form (eg /foo).
 * This is useful for supporting consistent uri's and to avoid checking for the
 * slashes everywhere
 */
@Provider
public class RemoveSlashFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        final String uri = request.getUriInfo().getRequestUri().getRawPath();

        if (uri.endsWith("/") && uri.length() > 1) {
            URI unslashed = URI.create(uri.substring(0, uri.length() - 1));
            Response response = Response.temporaryRedirect(unslashed).build();
            throw new WebApplicationException(response);
        }
    }
}