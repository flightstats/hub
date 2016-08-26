package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@Path("/internal/stacktrace")
public class InternalStacktraceResource {
    private final static Logger logger = LoggerFactory.getLogger(InternalStacktraceResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = InternalTracesResource.serverAndServers("/internal/stacktrace");
        try {
            Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
            for (Thread thread : threadMap.keySet()) {
                StackTraceElement[] elements = threadMap.get(thread);
                Thread.State state = thread.getState();
                ArrayNode stateArray = root.withArray(state.toString());
                ObjectNode threadNode = stateArray.addObject();
                ArrayNode stacktrace = threadNode.putArray(thread.getName());
                if (elements.length > 0) {
                    String first = elements[0].toString();
                    if (first.startsWith("sun.nio.ch")) {
                        stacktrace.add(first);
                        stacktrace.add("...");
                    } else if (first.startsWith("sun.misc.Unsafe.park")) {
                        stacktrace.add(first);
                        stacktrace.add("...");
                        for (StackTraceElement element : elements) {
                            String string = element.toString();
                            if (string.startsWith("com.flightstats")) {
                                stacktrace.add(string);
                            }
                        }
                    } else {
                        for (StackTraceElement element : elements) {
                            String string = element.toString();
                            stacktrace.add(string);
                            if (string.startsWith("sun.reflect.NativeMethodAccessorImpl.invoke")
                                    || string.startsWith("org.apache.curator.framework")
                                    || string.startsWith("java.util.concurrent.FutureTask")) {
                                stacktrace.add("...");
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("?", e);
        }
        return Response.ok(root).build();
    }
}
