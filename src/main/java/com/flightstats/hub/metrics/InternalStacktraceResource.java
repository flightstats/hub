package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Path("/internal/stacktrace")
public class InternalStacktraceResource {

    private final InternalTracesResource internalTracesResource;
    private final ObjectMapper objectMapper;

    @Inject
    public InternalStacktraceResource(InternalTracesResource internalTracesResource,
                                  ObjectMapper objectMapper) {
        this.internalTracesResource = internalTracesResource;
        this.objectMapper = objectMapper;
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = this.internalTracesResource.serverAndServers("/internal/stacktrace");
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
            log.warn("?", e);
        }
        return Response.ok(root).build();
    }

    private void mapThread(Thread thread, StackTraceElement[] elements, Map<String, ArrayNode> threadTraces) {
        ArrayNode stacktrace = this.objectMapper.createArrayNode();
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
