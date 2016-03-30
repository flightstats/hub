package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Trace;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);

    private static final String CHANNEL = "channel";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    private static final String MILISECONDS = "miliseconds";

    private static final String REQUEST_LATEST = "latest";
    private static final String REQUEST_EARLIEST = "earliest";
    private static final String REQUEST_BULK = "bulk";
    private static final String REQUEST_WS = "ws";
    private static final String REQUEST_EVENTS = "events";
    private static final String REQUEST_TIME = "time";
    private static final String REQUEST_STATUS = "status";

    public static final String HUB_DATADOG_METRICS_FLAG = "data_dog.enable";

    private final static StatsDClient statsd = new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"});
    private static final ThreadLocal<Traces> threadLocal = new ThreadLocal();
    private final boolean isDataDogActive;

    public DataDogRequestFilter() {
        isDataDogActive = HubProperties.getProperty(HUB_DATADOG_METRICS_FLAG, false);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (isDataDogActive) {
            Object object = threadLocal.get();
            if (object != null) {
                Traces traces = (Traces) object;
                traces.end();
                ListIterator<Trace> tracesListIt = traces.getTracesListIterator();
                while (tracesListIt.hasNext()) {
                    Trace aTrace = tracesListIt.next();
                    String context = aTrace.context();
                    long time = traces.getTime();
                    statsd.recordExecutionTime("hubRequest", time, new String[]{"endpoint:" + context});
                    logger.debug("Sending executionTime to DataDog for {} with time of {}.", context, time);
                }
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (isDataDogActive) {
            String method = request.getMethod();
            String servicePath = constructDeclaredPath(request);
            Traces aTraces = new Traces(method);
            aTraces.add(servicePath);
            threadLocal.set(aTraces);
        }
    }

    /**
     * Desconstructs the request path and reconstructs it as a template path down to the second
     * as well as append the method to the end.
     *
     * @param request
     * @return
     */
    private String constructDeclaredPath(ContainerRequestContext request) {
        URI uri = request.getUriInfo().getRequestUri();
        String method = request.getMethod();
        String path = uri.getPath();
        return constructDeclaredpath(path, method);
    }

    /**
     * Desconstructs path into a templated path with marker indicators for date time periods, i.e. year,
     * month, day, hour, etc.
     * <p>
     * Encountered query key words are included in the templated path.
     *
     * @param path
     * @param method
     * @return
     */
    protected String constructDeclaredpath(String path, String method) {
        logger.debug("Constructing template path from {} with method {}.", path, method);
        StringBuffer sbuff = new StringBuffer();
        String[] splits = path.split("\\/");
        for (int i = 0; i < splits.length; i++) {
            if (splits[i].equals(REQUEST_LATEST) || splits[i].equals(REQUEST_EARLIEST)
                    || splits[i].equals(REQUEST_BULK) || splits[i].equals(REQUEST_WS)
                    || splits[i].equals(REQUEST_EVENTS) || splits[i].equals(REQUEST_TIME)
                    || splits[i].equals(REQUEST_STATUS)) {

                sbuff.append("/").append(splits[i]);
            } else {
                switch (i) {
                    case 0:
                        sbuff.append(splits[i]);
                        break;
                    case 1:
                        sbuff.append("/").append(CHANNEL);
                        break;
                    case 2: // year
                        sbuff.append("/").append(YEAR);
                        break;
                    case 3: // Month
                        sbuff.append("/").append(MONTH);
                        break;
                    case 4: // day
                        sbuff.append("/").append(DAY);
                        break;
                    case 5: // hour
                        sbuff.append("/").append(HOUR);
                        break;
                    case 6: // minute
                        sbuff.append("/").append(MINUTE);
                        break;
                    case 7: // second
                        sbuff.append("/").append(SECOND);
                        break;
                    case 8: // miliseconds
                        sbuff.append("/").append(MILISECONDS);
                        break;
                }
            }
        }

        logger.debug("Generated template path: {}", sbuff.toString());
        return sbuff.toString();
    }


}
