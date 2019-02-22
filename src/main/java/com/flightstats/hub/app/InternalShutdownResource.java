package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * ShutdownResource should only be called from the localhost.
 */
@SuppressWarnings("WeakerAccess")
@Path("/internal/shutdown")
public class InternalShutdownResource {
    public static final String DESCRIPTION = "See if any server is being shutdown, shutdown a node, and reset the shutdown lock.";
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    private static ShutdownManager getManager() {
        return HubProvider.getInstance(ShutdownManager.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) throws Exception {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        try {
            root.put("shutdownLock", getManager().getLockData());
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
        return LocalHostOnly.getResponse(uriInfo, () -> getManager().shutdown(true));
    }

    @POST
    @Path("resetLock")
    public Response resetLock(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> getManager().resetLock());
    }

}
