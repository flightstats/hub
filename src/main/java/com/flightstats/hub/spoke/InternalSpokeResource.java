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

@SuppressWarnings("WeakerAccess")
@Path("/internal/spoke")
public class InternalSpokeResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalSpokeResource.class);
    private static final FileSpokeStore singleSpokeStore = HubProvider.getInstance(FileSpokeStore.class, FileSpokeStore.SINGLE);
    private static final FileSpokeStore batchSpokeStore = HubProvider.getInstance(FileSpokeStore.class, FileSpokeStore.BATCH);
    private static final RemoteSpokeStore remoteSpokeStore = HubProvider.getInstance(RemoteSpokeStore.class);

    @Context
    private UriInfo uriInfo;

    // TODO: remove these after migrating
    // ----------------------------------

    @GET
    @Path("/payload/{path:.+}")
    public Response oldGetPayloadMethod(@PathParam("path") String path) {
        return getPayload("single", path);
    }

    @PUT
    @Path("/payload/{path:.+}")
    public Response oldPutPayloadMethod(@PathParam("path") String path, InputStream input) {
        return putPayload("single", path, input);
    }

    @PUT
    @Path("/bulkKey/{channel}")
    public Response oldPutBulkMethod(@PathParam("channel") String channel, InputStream input) {
        return putBulk("single", channel, input);
    }

    @GET
    @Path("/time/{C}/{Y}/{M}/{day}")
    public Response oldTimeBucketMethod(@PathParam("C") String C,
                                        @PathParam("Y") String Y,
                                        @PathParam("M") String M,
                                        @PathParam("day") String day) {
        return getTimeBucket("single", C, Y, M, day);
    }

    @GET
    @Path("/time/{C}/{Y}/{M}/{D}/{hour}")
    public Response oldTimeBucketMethod(@PathParam("C") String C,
                                        @PathParam("Y") String Y,
                                        @PathParam("M") String M,
                                        @PathParam("D") String D,
                                        @PathParam("hour") String hour) {
        return getTimeBucket("single", C, Y, M, D, hour);
    }


    @GET
    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{minute}")
    public Response oldTimeBucketMethod(@PathParam("C") String C,
                                        @PathParam("Y") String Y,
                                        @PathParam("M") String M,
                                        @PathParam("D") String D,
                                        @PathParam("h") String h,
                                        @PathParam("minute") String minute) {
        return getTimeBucket("single", C, Y, M, D, h, minute);
    }


    @GET
    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{m}/{second}")
    public Response oldTimeBucketMethod(@PathParam("C") String C,
                                        @PathParam("Y") String Y,
                                        @PathParam("M") String M,
                                        @PathParam("D") String D,
                                        @PathParam("h") String h,
                                        @PathParam("m") String m,
                                        @PathParam("second") String second) {
        return getTimeBucket("single", C, Y, M, D, h, m, second);
    }

    @DELETE
    @Path("/payload/{path:.+}")
    public Response oldDeleteMethod(@PathParam("path") String path) {
        return delete("single", path);
    }

    // ----------------------------------

    @GET
    @Path("/{storeName}/payload/{path:.+}")
    public Response getPayload(@PathParam("storeName") String storeName,
                               @PathParam("path") String path) {
        try {
            FileSpokeStore store = getSpokeStoreByName(storeName);
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                try (OutputStream output = new BufferedOutputStream(os)) {
                    store.read(path, output);
                } catch (NotFoundException e) {
                    logger.debug("not found {}", e.getMessage());
                }
            });
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{storeName}/payload/{path:.+}")
    public Response putPayload(@PathParam("storeName") String storeName,
                               @PathParam("path") String path,
                               InputStream input) {
        try {
            long start = System.currentTimeMillis();
            FileSpokeStore store = getSpokeStoreByName(storeName);
            if (store.insert(path, input)) {
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

    @Path("{storeName}/bulkKey/{channel}")
    @PUT
    public Response putBulk(@PathParam("storeName") String storeName,
                            @PathParam("channel") String channel,
                            InputStream input) {
        try {
            long start = System.currentTimeMillis();
            FileSpokeStore store = getSpokeStoreByName(storeName);
            ObjectInputStream stream = new ObjectInputStream(input);
            int items = stream.readInt();
            for (int i = 0; i < items; i++) {
                String keyPath = new String(readByesFully(stream));
                byte[] data = readByesFully(stream);
                String itemPath = channel + "/" + keyPath;
                if (!store.insert(itemPath, new ByteArrayInputStream(data))) {
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

    private Response getResponse(FileSpokeStore store, String path) {
        logger.trace("time {}", path);
        try {
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                BufferedOutputStream output = new BufferedOutputStream(os);
                store.readKeysInBucket(path, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private FileSpokeStore getSpokeStoreByName(String name) {
        switch (name) {
            case "single":
                return singleSpokeStore;
            case "batch":
                return batchSpokeStore;
            default:
                throw new IllegalArgumentException("unknown spoke store: " + name);
        }
    }

    @GET
    @Path("/{storeName}/time/{C}/{Y}/{M}/{day}")
    public Response getTimeBucket(@PathParam("storeName") String storeName,
                                  @PathParam("C") String C,
                                  @PathParam("Y") String Y,
                                  @PathParam("M") String M,
                                  @PathParam("day") String day) {
        FileSpokeStore store = getSpokeStoreByName(storeName);
        return getResponse(store, C + "/" + Y + "/" + M + "/" + day);
    }

    @GET
    @Path("/{storeName}/time/{C}/{Y}/{M}/{D}/{hour}")
    public Response getTimeBucket(@PathParam("storeName") String storeName,
                                  @PathParam("C") String C,
                                  @PathParam("Y") String Y,
                                  @PathParam("M") String M,
                                  @PathParam("D") String D,
                                  @PathParam("hour") String hour) {
        FileSpokeStore store = getSpokeStoreByName(storeName);
        return getResponse(store, C + "/" + Y + "/" + M + "/" + D + "/" + hour);
    }

    @GET
    @Path("/{storeName}/time/{C}/{Y}/{M}/{D}/{h}/{minute}")
    public Response getTimeBucket(@PathParam("storeName") String storeName,
                                  @PathParam("C") String C,
                                  @PathParam("Y") String Y,
                                  @PathParam("M") String M,
                                  @PathParam("D") String D,
                                  @PathParam("h") String h,
                                  @PathParam("minute") String minute) {
        FileSpokeStore store = getSpokeStoreByName(storeName);
        return getResponse(store, C + "/" + Y + "/" + M + "/" + D + "/" + h + "/" + minute);
    }

    @GET
    @Path("/{storeName}/time/{C}/{Y}/{M}/{D}/{h}/{m}/{second}")
    public Response getTimeBucket(@PathParam("storeName") String storeName,
                                  @PathParam("C") String C,
                                  @PathParam("Y") String Y,
                                  @PathParam("M") String M,
                                  @PathParam("D") String D,
                                  @PathParam("h") String h,
                                  @PathParam("m") String m,
                                  @PathParam("second") String second) {
        FileSpokeStore store = getSpokeStoreByName(storeName);
        return getResponse(store, C + "/" + Y + "/" + M + "/" + D + "/" + h + "/" + m + "/" + second);
    }

    @DELETE
    @Path("/{storeName}/payload/{path:.+}")
    public Response delete(@PathParam("storeName") String storeName,
                           @PathParam("path") String path) {
        try {
            FileSpokeStore store = getSpokeStoreByName(storeName);
            store.delete(path);
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
            String read = singleSpokeStore.getLatest(channel, path);
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
                singleSpokeStore.getNext(channel, startKey, count, output);
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
