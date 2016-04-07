package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Trace;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ListIterator;

/**
 * Filter class to handle intercepting requests and respones from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);

    public static final String CHANNEL = "C";
    public static final String YEAR = "Y";
    public static final String MONTH = "M";
    public static final String DAY = "D";
    public static final String HOUR = "h";
    public static final String MINUTE = "m";
    public static final String SECOND = "s";
    public static final String MILISECONDS = "ms";
    public static final String HASH = "hash";
    public static final String COUNT = "count";


    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_EARLIEST = "earliest";
    public static final String KEY_LATEST = "latest";
    public static final String KEY_BATCH = "batch";
    public static final String KEY_BULK = "bulk";
    public static final String KEY_EVENTS = "events";
    public static final String KEY_STATUS = "status";
    public static final String KEY_TIME = "time";
    public static final String KEY_SECOND = "second";
    public static final String KEY_MINUTE = "minute";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_DAY = "day";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_TAG = "tag";
    public static final String KEY_INTERNAL = "internal";
    public static final String KEY_ZOOKEEPER = "zookeeper";
    public static final String KEY_S3BATCH = "s3Batch";
    public static final String KEY_TRACES = "traces";
    public static final String KEY_REPLICATION = "replication";
    public static final String KEY_SPOKE = "spoke";
    public static final String KEY_PAYLOAD = "payload";
    public static final String KEY_NEXT = "next";
    //    public static final String KEY_TEST = "test";
    public static final String KEY_MILLIS = "millis";
    public static final String KEY_REMOTE = "remote";
    public static final String KEY_LOCAL = "local";
    public static final String KEY_WS = "ws";
    public static final String KEY_GROUP = "group";
    public static final String KEY_HEALTH = "health";
    public static final String KEY_METRICS = "metrics";
    public static final String KEY_TRACE = "trace";
    public static final String KEY_PREVIOUS = "previous";

    private static final String[] RESERVED_WORDS = {KEY_CHANNEL, KEY_EARLIEST, KEY_LATEST, KEY_BATCH, KEY_BULK, KEY_EVENTS,
            KEY_STATUS, KEY_TIME, KEY_SECOND, KEY_MINUTE, KEY_HOUR, KEY_DAY, KEY_PROVIDER, KEY_TAG, KEY_INTERNAL, KEY_ZOOKEEPER,
            KEY_S3BATCH, KEY_TRACES, KEY_REPLICATION, KEY_SPOKE, KEY_PAYLOAD, KEY_NEXT, KEY_MILLIS, KEY_REMOTE,
            KEY_LOCAL, KEY_WS, KEY_GROUP, KEY_HEALTH, KEY_METRICS, KEY_TRACE, KEY_PREVIOUS};

//    private static final String[] RESERVED_WORDS = {"channel", "earliest", "latest", "batch", "bulk", "events", "status",
//            "time", "second", "minute", "hour", "day", "provider", "tag", "internal", "zookeeper", "s3Batch",
//            "traces", "replication", "spoke", "payload", "next", "test", "millis", "remote", "local", "ws",
//            "group", "health", "metrics", "trace"
//    };

    private static final String[] OPTIONAL_COUNT_WORDS = {KEY_NEXT, KEY_PREVIOUS, KEY_EARLIEST, KEY_LATEST};


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
     * Patterns:
     *  /internal/:type (ws):/id
     *  /internal/:type (spoke):/:function (payload):/path
     *  /internal/:type (spoke):/:function (time, next, latest):/timeBasedPath
     *  /internal/:type (spoke):/:function (next):/channel/count/startkey
     *  /internal/:type (spoke, replication, payload, time, latest):/timeBasedPath
     *
     *  /channel/channelName/timeBasedPath
     *  /channel/channelName/:function (bulk, latest)?:
     *  /channel/channelName/time/hour
     *
     *  /health
     *
     *  /tag
     *  /tag/:tagName:/timeDatePath
     *  /tag/:tagName:/timeDatePath/:direction:/:count:
     *  /tag/:tagName:/latest
     *
     * @param path
     * @param method
     * @return
     */
    protected String constructDeclaredpath(String path, String method) {
        logger.debug("Constructing template path from {} with method {}.", path, method);

        StringBuilder sbuff = new StringBuilder();
        String[] splits = path.split("\\/");

        if (splits[0].equals("internal")) {
            handleInternalPath(sbuff, splits);
        } else if (splits[0].equals("health")) {

        } else if (splits[0].equals(KEY_CHANNEL) || splits[0].equals(KEY_TAG)) {  // channel request most likely
            handleTagOrChannelPath(sbuff, splits);
        }


//        } else if (splits[0].equals("tag")) {
//            handleTagPath(sbuff, splits);
//        }

        logger.debug("Generated template path: {}", sbuff.toString());
        return sbuff.toString();
    }

    private void handleTagOrChannelPath(StringBuilder sbuff, String[] path) {
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
                    sbuff.append("/").append(path[0]);
                } else if (i > 1) {
                    i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i, path.length)) - 1;
                }
            }
        }
    }

    /**
     * /tag
     * /tag/:tagName:/timeDatePath
     * /tag/:tagName:/timeDatePath/:direction:/:count:
     * /tag/:tagName:/latest
     * <p>
     * tag/test/latest?stable=true
     * tag/test/2016/04/01/17/59/54/656/KXo3du/previous/10000
     * tag/test/2016/04/01/14/48/23/468/E9Yfkc/next/100
     * tag/test/2016/03/30/12/43/33/430/y8t02V
     *
     * @param sbuff
     * @param path
     */
    private void handleTagPath(StringBuilder sbuff, String[] path) {
        for (int i = 0; i < path.length; i++) {
            if (isReservedWord(path[i])) {
                sbuff.append("/").append(path[i]);
            } else if (i == 1) {
                // assume this is the tag name
                sbuff.append("/tag");
            } else if (i > 1) {
                // it's either the time date path, direction, count
                if (path[i].startsWith("latest?")) {
                    sbuff.append("/latest");
                } else {
                    i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i, path.length));
                }
            }


        }
    }

    /**
     * /internal/:type (ws):/id
     * /internal/:type (spoke):/:function (payload):/path
     * /internal/:type (spoke):/:function (time, next, latest):/timeBasedPath
     * /internal/:type (spoke):/:function (next):/channel/count/startkey
     * /internal/:type (spoke, replication, payload, time, latest):/timeBasedPath
     *
     * @param sbuff
     * @param path
     */
    private void handleInternalPath(StringBuilder sbuff, String[] path) {
        for (int i = 0; i < path.length; i++) {
            if (isReservedWord(path[i])) {
                sbuff.append("/").append(path[i]);
                if (path[i].equals("payload")) {
                    // assume everything after payload is the payload path so we'll just shorthand it as payload
                    // this may be an invalid assumption
                    sbuff.append("/path");
                    return;
                } else if (path[i].equals("time")) {
                    // build the date/time based path
                    i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i + 1, path.length));
                }
            }
        }
    }

    /**
     * /channel/channelName/timeBasedPath
     * /channel/channelName/:function (bulk, latest)?:
     * /channel/channelName/time/hour
     *
     * @param sbuff
     * @param path
     */
    private void handleChannelPath(StringBuilder sbuff, String[] path) {
        sbuff.append(CHANNEL);
        for (Integer i = 0; i < path.length; i++) {
            if (isReservedWord(path[i])) {
                sbuff.append("/").append(path[i]);
                if (path[i].equals("time") && i < path.length - 1) {// grab the hour
                    sbuff.append("/").append("h");
                }
                break;
            } else {
                if (i == 0) {
                    sbuff.append("/").append(CHANNEL);
                } else {
                    i += appendDateTimePath(sbuff, Arrays.copyOfRange(path, i, path.length)) - 1; // minus 1 becuase it's zero based
                }
            }
        }

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
                        sbuff.append("/").append(MILISECONDS);
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
        return ArrayUtils.contains(RESERVED_WORDS, aWord);
    }

    private boolean isOptionalParamWord(String aWord) {
        return ArrayUtils.contains(OPTIONAL_COUNT_WORDS, aWord);
    }

    private boolean isNumber(String aString) {
        return StringUtils.isNumeric(aString);
    }

}
