package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class SpokeTracer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTracer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public SpokeTracer() {
        HubServices.register(new SpokeTracerService());
    }

    private static Map<String, SpokeRequest> inProcess = new ConcurrentHashMap<>();
    private static volatile Map<String, SpokeRequest> completed = new ConcurrentHashMap<>();

    public static SpokeRequest start(String path, String method) {
        SpokeRequest request = new SpokeRequest(path, method);
        inProcess.put(request.getKey(), request);
        return request;
    }

    public static void completed(SpokeRequest request) {
        inProcess.remove(request.getKey());
        completed.put(request.getKey(), request);
        logger.trace("completed {}", request);
    }

    public static ObjectNode getTraces() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode inProcessNode = root.putArray("inProcess");
        getSorted(inProcess).forEach((request) -> inProcessNode.add(request.toNode()));
        ArrayNode completedNode = root.putArray("completed");
        getSorted(completed).forEach((request) -> completedNode.add(request.toNode()));
        return root;
    }

    private class SpokeTracerService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() throws Exception {
            Map<String, SpokeRequest> oldCompleted = completed;
            completed = new ConcurrentHashMap<>();

            logger.debug("inProcess traces:");
            getSorted(SpokeTracer.inProcess).forEach((request) -> {
                logger.debug("\t {}", request);
            });
            logger.debug("completed traces:");
            getSorted(oldCompleted).forEach((request) -> {
                logger.debug("\t {}", request);
            });
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
        }

    }

    private static TreeSet<SpokeRequest> getSorted(Map<String, SpokeRequest> map) {
        TreeSet<SpokeRequest> sorted = new TreeSet<>(map.values());
        map.forEach((key, request) -> {
            sorted.add(request);
        });
        return sorted;
    }

}
