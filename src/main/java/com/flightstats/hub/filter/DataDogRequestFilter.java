package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Trace;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.ListIterator;

/**
 * Filter class to handle intercepting requests and respones from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static StatsDClient statsd = new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"});

    private static final ThreadLocal<Traces> threadLocal = new ThreadLocal();

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        Object object = threadLocal.get();
        if (object != null) {
            Traces traces = (Traces) object;
            traces.end();

            ListIterator<Trace> tracesListIt = traces.getTracesListIterator();

            StringBuffer sbuff = new StringBuffer();
            // should really only be one item, however this could lead to aggregation
            while (tracesListIt.hasNext()) {
                Trace aTrace = tracesListIt.next();
                sbuff.append(aTrace.context());
            }

            statsd.recordExecutionTime(sbuff.toString(), traces.getTime());
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        URI requestUri = request.getUriInfo().getRequestUri();
        String path = requestUri.getPath();
        String method = request.getMethod();

        Traces aTraces = new Traces(method);
        threadLocal.set(aTraces);
    }
}
