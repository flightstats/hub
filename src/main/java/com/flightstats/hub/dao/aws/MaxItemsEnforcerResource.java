package com.flightstats.hub.dao.aws;

import com.flightstats.hub.rest.PATCH;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static javax.ws.rs.core.Response.ok;

@Slf4j
@Path("/internal/max-items")
public class MaxItemsEnforcerResource {

    private final S3Config s3Config;

    @Inject
    public MaxItemsEnforcerResource(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @PATCH
    @Path("/{channel}")
    public Response enforce(@PathParam("channel") String channel) {
        try {
            S3Config.S3ConfigLockable s3ConfigLockable = s3Config.new S3ConfigLockable(Collections.emptyList());
            s3ConfigLockable.updateMaxItems(channel);
            return ok().build();
        } catch (Exception e) {
            log.warn("unable to complete max items enforcer for " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }

}
