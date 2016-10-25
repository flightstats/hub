package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/internal/s3Verifier")
public class S3VerifierResource {

    private final static Logger logger = LoggerFactory.getLogger(S3VerifierResource.class);
    private static final S3Verifier s3Verifier = HubProvider.getInstance(S3Verifier.class);

    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel) {
        try {
            s3Verifier.verifyChannel(channel);
            return ok().build();
        } catch (Exception e) {
            logger.warn("unable to complete verification of " + channel, e);
            return Response.serverError().entity(e.getStackTrace()).build();
        }

    }
}
