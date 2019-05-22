package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.util.Commander;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Slf4j
@Path("/health/metrics")
public class MetricsResource {

    private final CustomMetricsReporter customMetricsReporter;
    private final ObjectMapper objectMapper;

    @Inject
    public MetricsResource(CustomMetricsReporter customMetricsReporter, ObjectMapper objectMapper) {
        this.customMetricsReporter = customMetricsReporter;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = this.objectMapper.createObjectNode();
        rootNode.put("openFiles", customMetricsReporter.getOpenFiles());
        return Response.ok(rootNode).build();
    }

    @GET
    @Path("trace")
    @Produces(MediaType.TEXT_PLAIN)
    public Response trigger() {
        return Response.ok(logFilesInfo()).build();
    }

    // TODO: Move(?), test, and refactor this
    private String logFilesInfo() {
        String info = "";
        log.info("logFilesInfo starting");
        info += "lsof -b -cjava : \r\n";
        info += Commander.run(new String[]{"lsof", "-b", "-cjava"}, 60);
        info += "thread dump \r\n";
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            StackTraceElement[] value = entry.getValue();
            info += entry.getKey().getName() + " : \r\n";
            for (StackTraceElement element : value) {
                info += "\t" + element.toString() + "\r\n";
            }
        }
        log.info("logFilesInfo completed");
        return info;
    }
}
