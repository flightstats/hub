package com.flightstats.hub.dao.aws;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Slf4j
@Path("/internal/s3Verifier")
public class InternalS3VerifierResource {

    private final S3Verifier s3Verifier;

    public InternalS3VerifierResource(S3Verifier s3Verifier) {
        this.s3Verifier = s3Verifier;
    }

    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel) {
        try {
            s3Verifier.verifyChannel(channel);
            return ok().build();
        } catch (Exception e) {
            log.warn("unable to complete verification of " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }
}