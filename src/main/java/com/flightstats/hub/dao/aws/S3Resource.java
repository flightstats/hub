package com.flightstats.hub.dao.aws;

import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/internal/s3")
public class S3Resource {

    @Inject
    private S3Config s3Config;

    @GET
    @Path("cleanup")
    public Response get() {
        s3Config.doWork();
        return Response.ok().build();
    }
}
