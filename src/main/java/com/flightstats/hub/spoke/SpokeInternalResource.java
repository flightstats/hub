package com.flightstats.hub.spoke;


import com.flightstats.hub.model.SingleTrace;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.Arrays;

@Path("/internal/spoke")
public class SpokeInternalResource {

    private final static Logger logger = LoggerFactory.getLogger(SpokeInternalResource.class);
    @Inject
    private FileSpokeStore spokeStore;
    @Inject
    private RemoteSpokeStore remoteSpokeStore;
    @Inject
    private UriInfo uriInfo;

    @Path("/payload/{path:.+}")
    @GET
    public Response getPayload(@PathParam("path") String path) {
        try {
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                BufferedOutputStream output = new BufferedOutputStream(os);
                spokeStore.read(path, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/payload/{path:.+}")
    @PUT
    public Response putPayload(@PathParam("path") String path, InputStream input) {
        try {
            DateTime start = TimeUtil.now();
            if (spokeStore.write(path, input)) {
                return Response
                        .created(uriInfo.getRequestUri())
                        .entity(new SingleTrace("success", start).toString())
                        .build();
            }
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new SingleTrace("failed", start).toString())
                    .build();
        } catch (Exception e) {
            logger.warn("unable to write " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response getResponse(String path) {
        logger.trace("time {}", path);
        try {
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                BufferedOutputStream output = new BufferedOutputStream(os);
                spokeStore.readKeysInBucket(path, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/time/{C}/{Y}/{M}/{day}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("day") String day) {
        return getResponse(C + "/" + Y + "/" + M + "/" + day);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{hour}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("hour") String hour) {
        return getResponse(C + "/" + Y + "/" + M + "/" + D + "/" + hour);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{minute}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("h") String h, @PathParam("minute") String minute) {
        return getResponse(C + "/" + Y + "/" + M + "/" + D + "/" + h + "/" + minute);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{m}/{second}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("h") String h, @PathParam("m") String m,
                                  @PathParam("second") String second) {
        return getResponse(C + "/" + Y + "/" + M + "/" + D + "/" + h + "/" + m + "/" + second);
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

    @Path("/latest/{channel}/{path:.+}")
    @GET
    public Response getLatest(@PathParam("channel") String channel, @PathParam("path") String path) {
        try {
            String read = spokeStore.getLatest(channel, path);
            if (read == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(read).build();
        } catch (NullPointerException e) {
            logger.debug("NPE - unable to get latest " + channel + " " + path);
        } catch (Exception e) {
            logger.warn("unable to get latest " + channel + " " + path, e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @Path("/next/{channel}/{count}/{startKey:.+}")
    @GET
    public Response getNext(@PathParam("channel") String channel, @PathParam("count") int count,
                            @PathParam("startKey") String startKey) {
        try {
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                BufferedOutputStream output = new BufferedOutputStream(os);
                spokeStore.getNext(channel, startKey, count, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to get next " + channel + " " + startKey, e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @Path("/test/{server}")
    @GET
    public Response test(@PathParam("server") String server) {
        logger.info("testing server {}", server);
        try {
            remoteSpokeStore.testOne(Arrays.asList(server));
            return Response.ok().build();
        } catch (Exception e) {
            logger.warn("unable to complete calls " + server, e);
        }
        return Response.status(417).build();
    }
}
