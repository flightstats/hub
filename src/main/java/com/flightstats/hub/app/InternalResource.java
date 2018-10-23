package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.channel.InternalChannelResource;
import com.flightstats.hub.cluster.InternalZookeeperResource;
import com.flightstats.hub.health.InternalHealthResource;
import com.flightstats.hub.metrics.InternalStacktraceResource;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.time.InternalTimeResource;
import com.flightstats.hub.webhook.InternalWebhookResource;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal")
public class InternalResource {

    private final ObjectMapper mapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalResource(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        ObjectNode root = mapper.createObjectNode();
        root.put("WARNING", "Internal APIs may change at any time. They are intended to be used interactively, and scripts might break at any time.");
        ObjectNode links = root.with("_links");

        String requestUri = StringUtils.appendIfMissing(uriInfo.getRequestUri().toString(), "/");
        links.with("self").put("href", requestUri);

        addLink(links, "channel", InternalChannelResource.DESCRIPTION, requestUri);
        addLink(links, "cluster", InternalClusterResource.DESCRIPTION, requestUri);
        addLink(links, "deploy", InternalDeployResource.DESCRIPTION, requestUri);
        addLink(links, "health", InternalHealthResource.DESCRIPTION, requestUri);
        addLink(links, "properties", InternalPropertiesResource.DESCRIPTION, requestUri);
        addLink(links, "shutdown", InternalShutdownResource.DESCRIPTION, requestUri);
        addLink(links, "stacktrace", InternalStacktraceResource.DESCRIPTION, requestUri);
        addLink(links, "time", InternalTimeResource.DESCRIPTION, requestUri);
        addLink(links, "traces", InternalTracesResource.DESCRIPTION, requestUri);
        addLink(links, "webhook", InternalWebhookResource.DESCRIPTION, requestUri);
        addLink(links, "zookeeper", InternalZookeeperResource.DESCRIPTION, requestUri);

        return Response.ok(root).build();
    }

    private void addLink(ObjectNode links, String name, String description, String requestUri) {
        ObjectNode node = links.with(name);
        node.put("description", description);
        node.put("href", requestUri + name);
    }

}
