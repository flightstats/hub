package com.flightstats.hub.spoke;

import com.flightstats.hub.model.SingleTrace;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.Collections;

import static com.flightstats.hub.constant.NamedBinding.READ;
import static com.flightstats.hub.constant.NamedBinding.WRITE;

@Slf4j
@Path("/internal/spoke")
public class InternalSpokeResource {

    private final FileSpokeStore writeSpokeStore;
    private final FileSpokeStore readSpokeStore;
    private final SpokeClusterHealthCheck healthCheck;

    @Context
    private UriInfo uriInfo;

    @Inject
    public InternalSpokeResource(@Named(WRITE) FileSpokeStore writeSpokeStore,
                                 @Named(READ) FileSpokeStore readSpokeStore,
                                 SpokeClusterHealthCheck healthCheck) {
        this.writeSpokeStore = writeSpokeStore;
        this.readSpokeStore = readSpokeStore;
        this.healthCheck = healthCheck;
    }

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
                    log.debug("not found {}", e.getMessage());
                }
            });
            return builder.build();
        } catch (Exception e) {
            log.warn("unable to get " + path, e);
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
                    log.info("slow write response {} {}", path, new DateTime(start));
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
            log.warn("unable to write " + path, e);
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
                    log.warn("what happened?!?! {}", channel);
                    return Response
                            .status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new SingleTrace("failed", start).toString())
                            .build();
                }
            }
            long end = System.currentTimeMillis();
            if ((end - start) > 4000) {
                log.info("slow bulk write response {} {}", channel, new DateTime(start));
            }
            return Response
                    .created(uriInfo.getRequestUri())
                    .entity(new SingleTrace("success", start).toString())
                    .build();
        } catch (Exception e) {
            log.warn("unable to write " + channel, e);
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
        log.trace("time {}", path);
        try {
            Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
                BufferedOutputStream output = new BufferedOutputStream(os);
                store.readKeysInBucket(path, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            log.warn("unable to get " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private FileSpokeStore getSpokeStoreByName(String name) {
        switch (SpokeStore.from(name)) {
            case WRITE:
                return writeSpokeStore;
            case READ:
                return readSpokeStore;
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
            log.warn("unable to write " + path, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/latest/{channel}/{path:.+}")
    @GET
    public Response getLatest(@PathParam("channel") String channel, @PathParam("path") String path) {
        try {
            String read = writeSpokeStore.getLatest(channel, path);
            if (read == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(read).build();
        } catch (Exception e) {
            log.warn("unable to get latest " + channel + " " + path, e);
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
                writeSpokeStore.getNext(channel, startKey, count, output);
                output.flush();
            });
            return builder.build();
        } catch (Exception e) {
            log.warn("unable to get next " + channel + " " + startKey, e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @Path("/test/{server}")
    @GET
    public Response test(@PathParam("server") String server) {
        log.info("testing server {}", server);
        try {
            healthCheck.testOne(Collections.singletonList(server));
            return Response.ok().build();
        } catch (Exception e) {
            log.warn("unable to complete calls " + server, e);
        }
        return Response.status(417).build();
    }
}