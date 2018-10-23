package com.flightstats.hub.dao.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/internal/s3Verifier")
public class InternalS3VerifierResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalS3VerifierResource.class);

    private final S3Verifier s3Verifier;

    @Inject
    InternalS3VerifierResource(S3Verifier s3Verifier) {
        this.s3Verifier = s3Verifier;
    }

    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel) {
        try {
            s3Verifier.verifyChannel(channel);
            return ok().build();
        } catch (Exception e) {
            logger.warn("unable to complete verification of " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }
    }
}
