package com.flightstats.hub.spoke;


import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * todo - gfm - 7/8/15 - After SpokeInternalResource is rolled out to all Hubs, we can update the url in RemoteSpokeStore and remove SpokeResource
 */
@Path("/spoke")
public class SpokeResource {

    private final static Logger logger = LoggerFactory.getLogger(SpokeResource.class);

    private SpokeInternalResource spoke;

    @Inject
    public SpokeResource(SpokeInternalResource spoke) {
        this.spoke = spoke;
    }

    @Path("/payload/{path:.+}")
    @GET
    public Response getPayload(@PathParam("path") String path) {
        return spoke.getPayload(path);
    }

    @Path("/payload/{path:.+}")
    @PUT
    public Response putPayload(@PathParam("path") String path, byte[] data) {
        return spoke.putPayload(path, data);
    }


    @Path("/time/{C}/{Y}/{M}/{day}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("day") String day) {
        return spoke.getTimeBucket(C, Y, M, day);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{hour}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("hour") String hour) {
        return spoke.getTimeBucket(C, Y, M, D, hour);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{minute}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("h") String h, @PathParam("minute") String minute) {
        return spoke.getTimeBucket(C, Y, M, D, h, minute);
    }

    @Path("/time/{C}/{Y}/{M}/{D}/{h}/{m}/{second}")
    @GET
    public Response getTimeBucket(@PathParam("C") String C, @PathParam("Y") String Y,
                                  @PathParam("M") String M, @PathParam("D") String D,
                                  @PathParam("h") String h, @PathParam("m") String m,
                                  @PathParam("second") String second) {
        return spoke.getTimeBucket(C, Y, M, D, h, m, second);
    }

    @Path("/payload/{path:.+}")
    @DELETE
    public Response delete(@PathParam("path") String path) {
        return spoke.delete(path);
    }

    @Path("/latest/{channel}/{path:.+}")
    @GET
    public Response getLatest(@PathParam("channel") String channel, @PathParam("path") String path) {
        return spoke.getLatest(channel, path);
    }

}
