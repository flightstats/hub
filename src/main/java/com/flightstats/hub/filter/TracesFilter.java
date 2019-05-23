package com.flightstats.hub.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.metrics.ActiveTraces;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

@Provider
@Slf4j
public class TracesFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final ObjectMapper mapper = HubBindings.objectMapper();

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        log.trace("response {} {} {}", request.getMethod(), requestUri, response.getStatus());
        boolean trace = Boolean.valueOf(request.getUriInfo().getQueryParameters().getFirst("trace"));
        if (trace) {
            Object entity = response.getEntity();
            if (entity == null) {
                log.info("trace == null");
            } else {
                if (mapper.canSerialize(entity.getClass())) {
                    entity = mapper.writeValueAsString(entity);
                }
                log.info("trace {} {}", entity.getClass(), entity);
            }
        }
        Thread thread = Thread.currentThread();
        if (!ActiveTraces.end(trace, response.getStatus())) {
            log.debug("unable to end trace for {}", requestUri);
        }
        thread.setName(StringUtils.substringBefore(thread.getName(), "|"));
    }

    @Override
    public void filter(ContainerRequestContext request) {
        URI requestUri = request.getUriInfo().getRequestUri();
        String ipAddress = request.getHeaders().getFirst("X-Forwarded-For");
        log.trace("incoming {} {} {}", requestUri, request.getMethod(), ipAddress);
        Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "|" + request.getMethod() + "|" + requestUri);
        ActiveTraces.start(requestUri, request.getMethod(), ipAddress);
    }
}
