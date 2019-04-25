package com.flightstats.hub.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@SuppressWarnings("WeakerAccess")
@Path("/health")
public class HealthResource {

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final HubHealthCheck healthCheck = HubProvider.getInstance(HubHealthCheck.class);
    private static final HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);
    @Context
    private UriInfo uriInfo;
    private AppProperty appProperty;

    public HealthResource(AppProperty appProperty) {
        this.appProperty = appProperty;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        HealthStatus healthStatus = healthCheck.getStatus();
        rootNode.put("healthy", healthStatus.isHealthy());
        rootNode.put("description", healthStatus.getDescription());
        rootNode.put("version", hubVersion.getVersion());
        rootNode.put("readOnly", appProperty.isReadOnly());
        DateTime startTime = HubMain.getStartTime();
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
