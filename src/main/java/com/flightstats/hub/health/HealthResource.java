package com.flightstats.hub.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/health")
public class HealthResource {

    private final DateTime startTime;
    private final ObjectMapper mapper;
    private final HubHealthCheck healthCheck;
    private final HubVersion hubVersion;
    private final HubProperties hubProperties;

    @Context
    private UriInfo uriInfo;

    @Inject
    HealthResource(ObjectMapper mapper,
                   HubHealthCheck healthCheck,
                   HubVersion hubVersion,
                   @Named("StartTime") DateTime startTime,
                   HubProperties hubProperties) {
        this.startTime = startTime;
        this.mapper = mapper;
        this.healthCheck = healthCheck;
        this.hubVersion = hubVersion;
        this.hubProperties = hubProperties;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        HealthStatus healthStatus = healthCheck.getStatus();
        rootNode.put("healthy", healthStatus.isHealthy());
        rootNode.put("description", healthStatus.getDescription());
        rootNode.put("version", hubVersion.getVersion());
        rootNode.put("readOnly", hubProperties.isReadOnly());
        rootNode.put("startTime", startTime.toString());
        rootNode.put("upTimeHours", new Duration(startTime, TimeUtil.now()).getStandardHours());
        ObjectNode links = rootNode.putObject("_links");
        LinkBuilder.addLink(links, "metrics", uriInfo.getBaseUriBuilder().path("health").path("metrics").build());
        if (healthStatus.isHealthy()) {
            return Response.ok(rootNode).build();
        } else if (healthCheck.isShuttingDown()) {
            return Response.status(520).entity(rootNode).build();
        } else {
            return Response.serverError().entity(rootNode).build();
        }
    }

}
