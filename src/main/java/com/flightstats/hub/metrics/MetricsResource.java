package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.util.Commander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@Path("/health/metrics")
public class MetricsResource {
    private final static Logger logger = LoggerFactory.getLogger(MetricsResource.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static CustomMetricsReporter customMetricsReporter = HubProvider.getInstance(CustomMetricsReporter.class);
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
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
    String logFilesInfo() {
        String info = "";
        logger.info("logFilesInfo starting");
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
        logger.info("logFilesInfo completed");
        return info;
    }
}
