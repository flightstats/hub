package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Slf4j
@Path("/internal/batch")
public class InternalBatchResource {

    private final S3BatchContentDao s3BatchContentDao;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalBatchResource(S3BatchContentDao s3BatchContentDao) {
        this.s3BatchContentDao = s3BatchContentDao;
    }

    @DELETE
    @Path("/{channel}/{year}/{month}/{day}/{hour}/{minute}")
    Response archiveBatch(@PathParam("channel") String channel,
                          @PathParam("year") int year,
                          @PathParam("month") int month,
                          @PathParam("day") int day,
                          @PathParam("hour") int hour,
                          @PathParam("minute") int minute)
    {
        if (HubProperties.isProtected() && !LocalHostOnly.isLocalhost(uriInfo)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            ContentKey key = new ContentKey(year, month, day, hour, minute);
            s3BatchContentDao.archiveBatch(channel, key);
            return Response.status(Response.Status.OK).build();
        }
    }

}
