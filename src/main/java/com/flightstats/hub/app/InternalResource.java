package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.flightstats.hub.util.Constants.CHANNEL_DESCRIPTION;
import static com.flightstats.hub.util.Constants.CLUSTER_DESCRIPTION;
import static com.flightstats.hub.util.Constants.DEPLOY_DESCRIPTION;
import static com.flightstats.hub.util.Constants.HEALTH_DESCRIPTION;
import static com.flightstats.hub.util.Constants.PROPERTIES_DESCRIPTION;
import static com.flightstats.hub.util.Constants.SHUTDOWN_DESCRIPTION;
import static com.flightstats.hub.util.Constants.STACKTRACE_DESCRIPTION;
import static com.flightstats.hub.util.Constants.TIME_DESCRIPTION;
import static com.flightstats.hub.util.Constants.TRACES_DESCRIPTION;
import static com.flightstats.hub.util.Constants.WEBHOOK_DESCRIPTION;
import static com.flightstats.hub.util.Constants.ZOOKEEPER_DESCRIPTION;

@Path("/internal")
public class InternalResource {

    private final ObjectMapper objectMapper;

    @Inject
    public InternalResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Context
    private UriInfo uriInfo;
    private ObjectNode links;
    private String requestUri;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("WARNING", "Internal APIs may change at any time. They are intended to be used interactively, and scripts might break at any time.");
        links = root.with("_links");

        requestUri = StringUtils.appendIfMissing(uriInfo.getRequestUri().toString(), "/");
        links.with("self").put("href", requestUri);

        addLink("channel", CHANNEL_DESCRIPTION);
        addLink("cluster", CLUSTER_DESCRIPTION);
        addLink("deploy", DEPLOY_DESCRIPTION);
        addLink("health", HEALTH_DESCRIPTION);
        addLink("properties", PROPERTIES_DESCRIPTION);
        addLink("shutdown", SHUTDOWN_DESCRIPTION);
        addLink("stacktrace", STACKTRACE_DESCRIPTION);
        addLink("time", TIME_DESCRIPTION);
        addLink("traces", TRACES_DESCRIPTION);
        addLink("webhook", WEBHOOK_DESCRIPTION);
        addLink("zookeeper", ZOOKEEPER_DESCRIPTION);
        return Response.ok(root).build();
    }

    private void addLink(String name, String description) {
        final ObjectNode node = links.with(name);
        node.put("description", description);
        node.put("href", requestUri + name);
    }

}
