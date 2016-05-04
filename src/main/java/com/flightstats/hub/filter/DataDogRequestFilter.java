package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.SingleTrace;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter class to handle intercepting requests and respones from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static Map<String, QueryType> queryTypes = null;
    private static Map<String, QueryKey> queryKeys = null;

    static {
        queryTypes = new HashMap<>();
        Arrays.stream(QueryType.values()).forEach(queryType -> queryTypes.put(queryType.toString(), queryType));

        queryKeys = new HashMap<>();
        Arrays.stream(QueryKey.values()).forEach(queryKey -> queryKeys.put(queryKey.toString(), queryKey));
    }

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);

    private enum QueryType {
        channel, tag, zookeeper, s3Batch, replication,
        events, time, spoke, unkown, group;
    }

    private enum QueryKey {
        channel, earliest, latest, batch, bulk, events, status, time, second, minute, hour, day, provider, tag, internal,
        zookeeper, s3Batch, traces, replication, spoke, payload, next, millis, remote, local, ws, group, health, metrics,
        trace, previous;

        private static HashMap<String, QueryKey> optionalCountKeys = null;

        public static HashMap<String, QueryKey> getOptionalCountKeys() {
            if (optionalCountKeys == null) {
                optionalCountKeys = new HashMap<>();
                optionalCountKeys.put(next.toString(), next);
                optionalCountKeys.put(previous.toString(), next);
                optionalCountKeys.put(earliest.toString(), earliest);
                optionalCountKeys.put(latest.toString(), latest);
            }

            return optionalCountKeys;
        }
    }

    public static final String CHANNEL = "C";
    public static final String YEAR = "Y";
    public static final String MONTH = "M";
    public static final String DAY = "D";
    public static final String HOUR = "h";
    public static final String MINUTE = "m";
    public static final String SECOND = "s";
    public static final String MILLISECONDS = "ms";
    public static final String HASH = "hash";
    public static final String COUNT = "count";

    public static final String HUB_DATADOG_METRICS_FLAG = "data_dog.enable";

    private final static StatsDClient statsd = new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"});
    private static final ThreadLocal<SingleTrace> threadLocal = new ThreadLocal();
    private final boolean isDataDogActive;

    public DataDogRequestFilter() {
        isDataDogActive = HubProperties.getProperty(HUB_DATADOG_METRICS_FLAG, false);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (isDataDogActive) {
            SingleTrace trace = threadLocal.get();
            if (trace != null) {
                long time = System.currentTimeMillis() - trace.getTime();
                String context = trace.context();

                statsd.recordExecutionTime("hubRequest", time, new String[]{"endpoint:" + context});
                logger.trace("Sending executionTime to DataDog for {} with time of {}.", context, time);

                Object[] traceObjs = trace.getObjects();
                if (traceObjs != null && traceObjs.length == 2) {
                    statsd.recordExecutionTime("hubUrl", time, new String[]{"endpoint:" + traceObjs[1]});
                    logger.trace("Sending executionTime to DataDog for {} with time of {}.", traceObjs[1], time);
                }

                // report any errors
                int returnCode = response.getStatus();
                if (returnCode > 400 && returnCode != 404) {
                    statsd.recordExecutionTime("hubErrors", System.currentTimeMillis(), new String[]{"errorCode:" + returnCode});
                }
            }
        } else {
            logger.debug("DataDog logging disabled.");
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (isDataDogActive) {
            threadLocal.set(new SingleTrace(request.getMethod(), constructDeclaredPath(request)));
        }
    }

    /**
     * Deconstructs the request path and reconstructs it as a template path down to the second
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
     * Deconstructs path into a templated path with marker indicators for date time periods, i.e. year,
     * month, day, hour, etc.
     * <p>
     * Encountered query key words are included in the templated path.
     *
     *  @param path
     * @param method
     * @return
     */
    protected String constructDeclaredpath(String path, String method) {
        //logger.debug("Constructing template path from {} with method {}.", path, method);

        StringBuilder sbuff = new StringBuilder();
        String[] splits = path.split("\\/");
        if (splits != null && splits.length > 0) {
            QueryKey key = queryKeys.get(splits[0]);

            if (key == QueryKey.internal) {
                handleInternalPath(sbuff, splits);
            } else {
                handlePath(sbuff, splits);
            }
        }
        logger.trace("Generated template path: {}", sbuff);
        return sbuff.toString();
    }

    /**
     * Handles generic URI paths.
     *
     * @param sbuff
     * @param path
     */
    private void handlePath(StringBuilder sbuff, String[] path) {
        QueryType queryType = null;
        if (path.length > 1) {
            queryType = queryTypes.get(path[0]);
        }

        for (int i = 0; i < path.length; i++) {
            int paramIndex = path[i].indexOf("?");
            String pathItem = paramIndex == -1 ? path[i] : path[i].substring(0, paramIndex);
            if (isOptionalParamWord(pathItem) && i < path.length - 1) {
                sbuff.append("/").append(pathItem).append("/").append("count");
                i++;
            } else if (isReservedWord(pathItem)) {
                sbuff.append("/").append(pathItem);
                if (isOptionalParamWord(pathItem) && i < path.length - 1) {
                    sbuff.append("/").append(COUNT);
                    i++;
                }
            } else {
                if (i == 1) {
                    if (QueryType.group == queryType) {
                        sbuff.append("/name");
                    } else {
                        sbuff.append("/").append(path[0]);
                    }
                } else if (i > 1) {
                    i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i, path.length)) - 1;
                }
            }
        }
    }

    /**
     * Specifially handles paths begining with /internal
     *
     * @param sbuff
     * @param path
     */
    private void handleInternalPath(StringBuilder sbuff, String[] path) {
        QueryType queryType = QueryType.unkown;
        for (int i = 0; i < path.length; i++) {

            if (i == 0) {
                if (isReservedWord(path[i])) {
                    sbuff.append("/").append(path[i]);
                }
            } else if (i == 1) {
                QueryType aType = queryTypes.get(path[i]);
                if (aType != null) {
                    queryType = aType;
                }
                sbuff.append("/").append(path[i]);
            } else if (i > 1) {
                switch (queryType) {
                    case zookeeper:
                        sbuff.append("/path");
                        break;
                    case s3Batch:
                        sbuff.append("/").append(CHANNEL);
                        break;
                    case replication:
                        sbuff.append("/").append(CHANNEL);
                        break;
                    case events:
                        sbuff.append("/id");
                        break;
                    case spoke:
                        i += handleInternalSpoke(sbuff, Arrays.copyOfRange(path, i, path.length));
                        break;
                    default:
                        if (isReservedWord(path[i])) {
                            sbuff.append("/").append(path[i]);
                            if (path[i].equals(QueryKey.payload.toString())) {
                                // assume everything after payload is the payload path so we'll just shorthand it as payload
                                // this may be an invalid assumption
                                sbuff.append("/path");
                                return;
                            } else if (path[i].equals(QueryKey.time.toString())) {
                                // build the date/time based path
                                i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i + 1, path.length));
                            }
                        }
                }
            }
        }
    }

    private int handleInternalSpoke(StringBuilder sbuff, String[] path) {
        int i = 0;
        for (i = 0; i < path.length; i++) {
            if (isReservedWord(path[i])) {
                sbuff.append("/").append(path[i]);
            }

            QueryKey type = queryKeys.get(path[i]);
            if (type != null) {
                switch (type) {
                    case payload:
                        sbuff.append("/path");
                        i++;
                        break;
                    case time:
                        sbuff.append("/channel");
                        i++;
                        i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i + 1, path.length));
                        break;
                    case latest:
                        sbuff.append("/").append(CHANNEL).append("/").append(COUNT).append("key");
                        break;
                }
            }
        }
        return i;
    }

    /**
     * Handles templatizing the date/time path
     *
     * @param sbuff  - templated path is appended to sbuff
     * @param splits
     * @return i - the number of elements traversed
     */
    private int appendDateTimePath(StringBuilder sbuff, String[] splits) {
        int i = 0;
        while (i < splits.length) {
            if (isOptionalParamWord(splits[i])) {
                sbuff.append("/").append(splits[i]);
                if (i < splits.length - 1) {
                    sbuff.append("/").append("count");
                }
            } else {

                switch (i) {
                    case 0: // year
                        sbuff.append("/").append(YEAR);
                        break;
                    case 1: // Month
                        sbuff.append("/").append(MONTH);
                        break;
                    case 2: // day
                        sbuff.append("/").append(DAY);
                        break;
                    case 3: // hour
                        sbuff.append("/").append(HOUR);
                        break;
                    case 4: // minute
                        sbuff.append("/").append(MINUTE);
                        break;
                    case 5: // second
                        sbuff.append("/").append(SECOND);
                        break;
                    case 6: // milliseconds
                        sbuff.append("/").append(MILLISECONDS);
                        break;
                    case 7:
                        if (isReservedWord(splits[i])) {
                            sbuff.append("/").append(splits[i]);
                            break;
                        }

                        sbuff.append("/").append(HASH);
                        break;
                }
            }
            i++;
        }
        return i;
    }

    private boolean isReservedWord(String aWord) {
        return queryKeys.get(aWord) != null;
    }

    private boolean isOptionalParamWord(String aWord) {
        return QueryKey.getOptionalCountKeys().get(aWord) != null;
    }

}
