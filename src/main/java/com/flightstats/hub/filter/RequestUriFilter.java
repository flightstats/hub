package com.flightstats.hub.filter;

import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

@SuppressWarnings("WeakerAccess")
@Provider
@PreMatching()
@Singleton
public class RequestUriFilter implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestUriFilter.class);

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        MultivaluedMap<String, String> headers = request.getHeaders();
        UriInfo uriInfo = request.getUriInfo();
        if (headers.containsKey("X-Forwarded-Host")
                || headers.containsKey("X-Forwarded-Proto")) {
            String host = getHost(request);
            int port = -1;
            if (host.contains(":")) {
                port = Integer.parseInt(StringUtils.substringAfter(host, ":"));
                host = StringUtils.substringBefore(host, ":");
            }
            URI baseUri = uriInfo.getBaseUriBuilder()
                    .host(host)
                    .scheme(getScheme(request))
                    .port(port)
                    .build();
            URI requestUri = uriInfo.getRequestUriBuilder()
                    .host(host)
                    .scheme(getScheme(request))
                    .port(port)
                    .build();
            request.setRequestUri(baseUri, requestUri);
            logger.trace("new request URI {}", request.getUriInfo().getRequestUri());
        } else if (uriInfo.getBaseUri().getPort() == 80
                || uriInfo.getBaseUri().getPort() == 443) {
            request.setRequestUri(uriInfo.getBaseUriBuilder().port(-1).build(),
                    uriInfo.getRequestUriBuilder().port(-1).build());
            logger.trace("removed port {}", request.getUriInfo().getRequestUri());
        }
    }

    private String getHost(ContainerRequestContext request) {
        String host = request.getHeaderString("X-Forwarded-Host");
        if (StringUtils.isBlank(host)) {
            host = request.getUriInfo().getBaseUri().getHost();
        } else if (host.contains(",")) {
            host = StringUtils.substringBefore(host, ",");
        }
        return host;
    }

    private String getScheme(ContainerRequestContext request) {
        String protocol = request.getHeaderString("X-Forwarded-Proto");
        logger.trace("uri {} protocol {}", request.getUriInfo().getRequestUri(), protocol);
        if (StringUtils.isBlank(protocol)) {
            protocol = request.getUriInfo().getBaseUri().getScheme();
        } else if (protocol.contains(",")) {
            protocol = StringUtils.substringBefore(protocol, ",");
        }
        return protocol;
    }
}
