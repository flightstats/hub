package com.flightstats.hub.spoke;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/spoke")
public class SpokeResource {

    private final static Logger logger = LoggerFactory.getLogger(SpokeResource.class);
    private final FileSpokeStore spokeStore;
    private final UriInfo uriInfo;

    @Inject
    public SpokeResource(FileSpokeStore spokeStore, UriInfo uriInfo) {
        this.spokeStore = spokeStore;
        this.uriInfo = uriInfo;
    }

    @Path("/payload/{path:.+}")
    @GET
    public Response getPayload(@PathParam("path") String path) {
        try {
            byte[] read = spokeStore.read(path);
            if (read == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            //todo - gfm - 11/13/14 - this could verify bytes
            return Response.ok(read).build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/next/{path:.+}")
    @GET
    public Response getNext(@PathParam("path") String path) {
        try {
            String key = spokeStore.nextPath(path);
            if (key == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(key).build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/previous/{path:.+}")
    @GET
    public Response getPrevious(@PathParam("path") String path) {
        // TODO bc 11/18/14:  use lambdas on next and previous
        try {
            String key = spokeStore.previousPath(path);
            if (key == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(key).build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/time/{path:.+}")
    @GET
    public Response getTimeBucket(@PathParam("path") String path) {
        try {
            //todo - gfm - 11/17/14 - this can use String instead of bytes, could also use Json
            String read = spokeStore.readKeysInBucket(path);
            if (read == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(read).build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/payload/{path:.+}")
    @PUT
    public Response putPayload(@PathParam("path") String path, byte[] data) {
        try {
            if (spokeStore.write(path, data)) {
                return Response.created(uriInfo.getRequestUri()).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.warn("unable to write " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/payload/{path:.+}")
    @DELETE
    public Response delete(@PathParam("path") String path) {
        try {
            spokeStore.delete(path);
            return Response.ok().build();
        } catch (Exception e) {
            logger.warn("unable to write " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


}
