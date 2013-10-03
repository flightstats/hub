package com.flightstats.datahub.app.config;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Redirect from trailing slash URI (eg /foo/) to correct form (eg /foo).
 * This is useful for supporting users typing in addresses or "hacking" longer URIs.
 */
public class RemoveSlashFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RemoveSlashFilter.class);

    public ContainerRequest filter(ContainerRequest request) {
        final URI uri = request.getRequestUri();
        final String path = uri.getPath();
        if (path.endsWith("/")) {
            // For consistency, I redirect from the root / resource to /index.
            // This supports having addresses like /index.html and /index.atom.
            String unslashedPath = path.equals("/") ? "/index" : path.substring(0, path.length() - 1);

            try {
                URI unslashedURI = new URI(uri.getScheme(), uri.getAuthority(), unslashedPath, uri.getQuery(), uri.getFragment());
                Response response = Response.temporaryRedirect(unslashedURI).build();
                throw new WebApplicationException(response);
            } catch (URISyntaxException exception) {
                log.error("Error removing slash from {}", uri.toString());
            }
        }
        return request;
    }
}