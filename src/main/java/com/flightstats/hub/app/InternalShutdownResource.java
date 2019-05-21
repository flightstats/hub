package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.rest.Linked;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.hub.constant.InternalResourceDescription.SHUTDOWN_DESCRIPTION;

/**
 * ShutdownResource should only be called from the localhost.
 */
@Path("/internal/shutdown")
public class InternalShutdownResource {

    private final ShutdownManager shutdownManager;
    private final ObjectMapper objectMapper;

    @Inject
    public InternalShutdownResource(ShutdownManager shutdownManager, ObjectMapper objectMapper) {
        this.shutdownManager = shutdownManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("description", SHUTDOWN_DESCRIPTION);
        root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        try {
            root.put("shutdownLock", this.shutdownManager.getLockData());
        } catch (Exception e) {
            root.put("shutdownLock", "none");
        }
        String localhostLink = HubHost.getLocalhostUri() + requestUri.getPath();
        Linked.Builder<?> links = Linked.linked(root);
        links.withLink("self", requestUri);
        links.withLink("shutdown", localhostLink);
        links.withLink("resetLock", localhostLink + "/resetLock");
        return Response.ok(links.build()).build();
    }

    @POST
    public Response shutdown(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> this.shutdownManager.shutdown(true));
    }

    @POST
    @Path("resetLock")
    public Response resetLock(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> this.shutdownManager.resetLock());
    }

}
