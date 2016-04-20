package com.flightstats.hub.spoke;


import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.SingleTrace;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.util.Arrays;

@Path("/internal/spoke")
public class SpokeInternalResource {

    private final static Logger logger = LoggerFactory.getLogger(SpokeInternalResource.class);

    @Context
    private UriInfo uriInfo;

    private static final FileSpokeStore spokeStore = HubProvider.getInstance(FileSpokeStore.class);
    private static final RemoteSpokeStore remoteSpokeStore = HubProvider.getInstance(RemoteSpokeStore.class);

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
            long start = System.currentTimeMillis();
            if (spokeStore.write(path, input)) {
                long end = System.currentTimeMillis();
                if ((end - start) > 4000) {
                    logger.info("slow write response {} {}", path, new DateTime(start));
                }
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

    @Path("/bulkKey/{channel}")
    @PUT
    public Response putBulk(@PathParam("channel") String channel, InputStream input) {
        try {
            long start = System.currentTimeMillis();
            ObjectInputStream stream = new ObjectInputStream(input);
            int items = stream.readInt();
            for (int i = 0; i < items; i++) {
                String keyPath = new String(readByesFully(stream));
                byte[] data = readByesFully(stream);
                String itemPath = channel + "/" + keyPath;
                if (!spokeStore.write(itemPath, new ByteArrayInputStream(data))) {
                    logger.warn("what happened?!?! {}", channel);
                    return Response
                            .status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new SingleTrace("failed", start).toString())
                            .build();
                }
            }
            long end = System.currentTimeMillis();
            if ((end - start) > 4000) {
                logger.info("slow bulk write response {} {}", channel, new DateTime(start));
            }
            return Response
                    .created(uriInfo.getRequestUri())
                    .entity(new SingleTrace("success", start).toString())
                    .build();
        } catch (Exception e) {
            logger.warn("unable to write " + channel, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private byte[] readByesFully(ObjectInputStream stream) throws IOException {
        int size = stream.readInt();
        byte[] data = new byte[size];
        ByteStreams.readFully(stream, data);
        return data;
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
