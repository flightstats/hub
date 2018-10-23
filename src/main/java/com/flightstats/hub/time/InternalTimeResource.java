package com.flightstats.hub.time;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.util.TimeUtil;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal/time")
public class InternalTimeResource {

    public static final String DESCRIPTION = "Links for managing time in a hub cluster.";

    private final TimeService timeService;
    private final InternalTracesResource internalTracesResource;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalTimeResource(TimeService timeService, InternalTracesResource internalTracesResource) {
        this.timeService = timeService;
        this.internalTracesResource = internalTracesResource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {

        ObjectNode root = internalTracesResource.serverAndServers("/internal/time");
        root.put("description", DESCRIPTION);
        root.put("details", "There are occasions when we know a particular hub system will not have the correct cluster " +
                "time.  This interface allows an admin to tell a hub instance to use a remote system instead of trusting " +
                "it's local clock for time sensitive operations.");

        root.put("restrictions", "All HTTP PUT calls must be made from localhost. ");

        ObjectNode directions = root.putObject("directions");
        directions.put("millis", "HTTP GET to /internal/time/millis to system time in millis if local.");
        directions.put("local", "HTTP GET to /internal/time/local to local system time.  HTTP PUT to change system to use local time.");
        directions.put("remote", "HTTP GET to /internal/time/remote to a remote systems time. HTTP PUT to change system to use remote time.");

        ObjectNode links = root.putObject("_links");
        String uri = uriInfo.getRequestUri().toString();
        addLink(links, "self", uri);
        addLink(links, "millis", uri + "/millis");
        addLink(links, "local", uri + "/local");
        addLink(links, "remote", uri + "/remote");

        root.put("remote", timeService.isRemote());

        return Response.ok(root).build();
    }

    private void addLink(ObjectNode node, String key, String value) {
        ObjectNode link = node.putObject(key);
        link.put("href", value);
    }

    @GET
    @Path("/millis")
    public Response getMillis() {
        if (timeService.isRemote()) {
            return Response.status(521).build();
        }
        return Response.ok(TimeUtil.now().getMillis()).build();
    }

    @PUT
    @Path("/remote")
    public Response remote() throws Exception {
        return setRemote(true);
    }

    private Response setRemote(boolean remote) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> {
            timeService.setRemote(remote);
            return Response.ok().build();
        });
    }

    @GET
    @Path("/remote")
    public Response getRemote() {
        return Response.ok(timeService.getRemoteNow().getMillis()).build();
    }

    @PUT
    @Path("/local")
    public Response ok() throws Exception {
        return setRemote(false);
    }

    @GET
    @Path("/local")
    public Response getLocal() {
        return Response.ok(TimeUtil.now().getMillis()).build();
    }
}
