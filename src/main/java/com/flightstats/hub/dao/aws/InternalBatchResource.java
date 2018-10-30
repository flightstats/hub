package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Slf4j
@Path("/internal/batch")
public class InternalBatchResource {

    public static final String DESCRIPTION = "Perform administrative tasks against batch payloads";

    private final S3BatchContentDao s3BatchContentDao;
    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalBatchResource(S3BatchContentDao s3BatchContentDao, ObjectMapper objectMapper) {
        this.s3BatchContentDao = s3BatchContentDao;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response documentResource() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());

        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("archive/{batchPath}", "HTTP POST to /internal/batch/archive/{channel}/{year}/{month}/{day}/{hour}/{minute}");

        return Response.ok(root).build();
    }

    @POST
    @Path("/archive/{channel}/{year}/{month}/{day}/{hour}/{minute}")
    public Response archiveBatch(@PathParam("channel") String channel,
                          @PathParam("year") int year,
                          @PathParam("month") int month,
                          @PathParam("day") int day,
                          @PathParam("hour") int hour,
                          @PathParam("minute") int minute) {
        if (HubProperties.isProtected() && !LocalHostOnly.isLocalhost(uriInfo)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            ContentKey key = new ContentKey(year, month, day, hour, minute);
            s3BatchContentDao.archiveBatch(channel, key);
            return Response.status(Response.Status.OK).build();
        }
    }

}
