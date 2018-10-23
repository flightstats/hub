package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.channel.InternalChannelResource;
import com.flightstats.hub.cluster.InternalZookeeperResource;
import com.flightstats.hub.dao.aws.InternalBatchResource;
import com.flightstats.hub.health.InternalHealthResource;
import com.flightstats.hub.metrics.InternalStacktraceResource;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.time.InternalTimeResource;
import com.flightstats.hub.webhook.InternalWebhookResource;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@SuppressWarnings("WeakerAccess")
@Path("/internal")
public class InternalResource {

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @Context
    private UriInfo uriInfo;
    private ObjectNode links;
    private String requestUri;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        ObjectNode root = mapper.createObjectNode();
        root.put("WARNING", "Internal APIs may change at any time. They are intended to be used interactively, and scripts might break at any time.");
        links = root.with("_links");

        requestUri = StringUtils.appendIfMissing(uriInfo.getRequestUri().toString(), "/");
        links.with("self").put("href", requestUri);

        addLink("batch", InternalBatchResource.DESCRIPTION);
        addLink("channel", InternalChannelResource.DESCRIPTION);
        addLink("cluster", InternalClusterResource.DESCRIPTION);
        addLink("deploy", InternalDeployResource.DESCRIPTION);
        addLink("health", InternalHealthResource.DESCRIPTION);
        addLink("properties", InternalPropertiesResource.DESCRIPTION);
        addLink("shutdown", InternalShutdownResource.DESCRIPTION);
        addLink("stacktrace", InternalStacktraceResource.DESCRIPTION);
        addLink("time", InternalTimeResource.DESCRIPTION);
        addLink("traces", InternalTracesResource.DESCRIPTION);
        addLink("webhook", InternalWebhookResource.DESCRIPTION);
        addLink("zookeeper", InternalZookeeperResource.DESCRIPTION);
        return Response.ok(root).build();
    }

    private ObjectNode addLink(String name, String description) {
        ObjectNode node = links.with(name);
        node.put("description", description);
        node.put("href", requestUri + name);
        return node;
    }

}
