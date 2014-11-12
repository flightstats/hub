package com.flightstats.hub.spoke;

import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/spoke")
public class SpokeResource {

    private final SpokeFileStore spokeFileStore;
    private final UriInfo uriInfo;

    @Inject
    public SpokeResource(SpokeFileStore spokeFileStore, UriInfo uriInfo) {
        this.spokeFileStore = spokeFileStore;
        this.uriInfo = uriInfo;
    }

    @Path("/payload/{path}")
    @GET
    public Response getPayload(@PathParam("path") String path) {
        byte[] read = spokeFileStore.read(path);
        if (read == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(read).build();
    }

    @Path("/payload/{path}")
    @PUT
    public Response putPayload(@PathParam("path") String path, byte[] data) {
        if (spokeFileStore.write(path, data)) {
            return Response.created(uriInfo.getRequestUri()).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }


}
