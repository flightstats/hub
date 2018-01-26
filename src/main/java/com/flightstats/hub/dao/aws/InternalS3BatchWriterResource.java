package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/internal/s3BatchWriter")
public class InternalS3BatchWriterResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalS3BatchWriterResource.class);
    private static final S3BatchProcessor s3BatchProcessor = HubProvider.getInstance(S3BatchProcessor.class);

    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel) {
        try {
            s3BatchProcessor.writeChannel(channel);
            return ok().build();
        } catch (Exception e) {
            logger.warn("unable to complete verification of " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }
}
