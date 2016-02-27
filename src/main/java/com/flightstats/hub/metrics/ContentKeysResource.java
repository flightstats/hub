package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubBindings;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/internal/keys")
public class ContentKeysResource {

    private final static ObjectMapper mapper = HubBindings.objectMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels(@Context UriInfo uriInfo) {
        ObjectNode root = TracesResource.serverAndServers("/internal/keys");
        addItems(root, "active", ContentKeyMap.getActive());
        addItems(root, "topKeys", ContentKeyMap.getTop());
        return Response.ok(root).build();
    }

    private void addItems(ObjectNode root, String name, List<ContentKeyMapStack> items) {
        ArrayNode mostKeys = root.putArray(name);
        for (ContentKeyMapStack item : items) {
            ObjectNode itemNode = mostKeys.addObject();
            itemNode.put("name", item.getName());
            itemNode.put("count", item.getCount());
            itemNode.put("start", new DateTime(item.getStart()).toString());
            itemNode.put("end", new DateTime(item.getEnd()).toString());
            ArrayNode stacktrace = itemNode.putArray("stacktrace");
            StackTraceElement[] elements = item.getStacktrace();
            for (int i = 2; i < elements.length; i++) {
                StackTraceElement element = elements[i];
                String line = element.toString();
                if (line.startsWith("java.util.concurrent.Executors$RunnableAdapter")
                        || line.startsWith("org.glassfish.jersey.message.internal.StreamingOutputProvider")) {
                    stacktrace.add("...");
                    break;
                } else {
                    stacktrace.add(line);
                }
            }

        }
    }
}
