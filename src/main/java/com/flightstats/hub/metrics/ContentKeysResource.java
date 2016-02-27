package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.model.ContentKeyMap;
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
        ArrayNode mostKeys = root.putArray("mostKeys");
        List<ContentKeyMap.ContentKeyMapStack> topItems = ContentKeyMap.getTopItems(50);
        for (ContentKeyMap.ContentKeyMapStack item : topItems) {
            ObjectNode itemNode = mostKeys.addObject();
            itemNode.put("name", item.getName());
            itemNode.put("count", item.getCount());
            itemNode.put("start", new DateTime(item.getStart()).toString());
            ArrayNode stacktrace = itemNode.putArray("stacktrace");
            StackTraceElement[] elements = item.getStacktrace();
            for (StackTraceElement element : elements) {
                stacktrace.add(element.toString());
            }
        }

        return Response.ok(root).build();
    }
}
