package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpokeTracer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTracer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, SpokeRequest> inProcess = new ConcurrentHashMap<>();
    //public static Map<String, SpokeRequest> completed = new ConcurrentHashMap<>();

    public static SpokeRequest start(String path, String method) {
        SpokeRequest request = new SpokeRequest(path, method);
        inProcess.put(request.getKey(), request);
        return request;
    }

    public static void completed(SpokeRequest request) {
        inProcess.remove(request.getKey());
        //completed.put(request.getKey(), request);
    }

    //todo - gfm - 9/3/15 - this should query itself every minute
    //always log the inprocess requests
    //log completed items, then clear the map

    public static ObjectNode getTraces() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode inProcess = root.putArray("inProcess");
        SpokeTracer.inProcess.forEach((key, request) -> {
            inProcess.add(request.toNode());
        });
        return root;
    }

}
