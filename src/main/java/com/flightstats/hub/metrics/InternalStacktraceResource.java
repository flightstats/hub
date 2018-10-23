package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.TreeMap;

@Path("/internal/stacktrace")
public class InternalStacktraceResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalStacktraceResource.class);
    public final static String DESCRIPTION = "Get a condensed stacktrace with links to other hubs in the cluster.";

    private final ObjectMapper mapper;
    private final InternalTracesResource internalTracesResource;

    @Inject
    InternalStacktraceResource(ObjectMapper mapper, InternalTracesResource internalTracesResource) {
        this.mapper = mapper;
        this.internalTracesResource = internalTracesResource;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = internalTracesResource.serverAndServers("/internal/stacktrace");
        try {
            Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
            TreeMap<String, Map<String, ArrayNode>> threadStates = new TreeMap<>();
            for (Thread thread : threadMap.keySet()) {
                String state = thread.getState().toString();
                if (!threadStates.containsKey(state)) {
                    threadStates.put(state, new TreeMap<>());
                }
                Map<String, ArrayNode> traces = threadStates.get(state);
                mapThread(thread, threadMap.get(thread), traces);
            }
            for (String state : threadStates.keySet()) {
                Map<String, ArrayNode> arrayNodeMap = threadStates.get(state);
                ArrayNode stateArray = root.putArray(state);
                for (String thread : arrayNodeMap.keySet()) {
                    ObjectNode node = stateArray.addObject();
                    node.set(thread, arrayNodeMap.get(thread));
                }
            }
        } catch (Exception e) {
            logger.warn("?", e);
        }
        return Response.ok(root).build();
    }

    private void mapThread(Thread thread, StackTraceElement[] elements, Map<String, ArrayNode> threadTraces) {
        ArrayNode stacktrace = mapper.createArrayNode();
        threadTraces.put(thread.getName(), stacktrace);
        if (elements.length > 0) {
            filterElements(elements, stacktrace);
        }
    }

    private void filterElements(StackTraceElement[] elements, ArrayNode stacktrace) {
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
            writeElements(elements, stacktrace);
        }
    }

    private void writeElements(StackTraceElement[] elements, ArrayNode stacktrace) {
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
