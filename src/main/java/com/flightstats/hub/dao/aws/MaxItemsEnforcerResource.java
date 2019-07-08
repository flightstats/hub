package com.flightstats.hub.dao.aws;


import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Slf4j
@Path("/internal/max-items")
public class MaxItemsEnforcerResource {
    private final S3Config.S3ConfigLockable s3ConfigLockable;

    @Inject
    public MaxItemsEnforcerResource(S3Config s3Config,
                                    @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao) {
        this.s3ConfigLockable = s3Config.new S3ConfigLockable(channelConfigDao.getAll(false));
    }

    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel) {
        try {
            s3ConfigLockable.updateMaxItems();
            return ok().build();
        } catch (Exception e) {
            log.warn("unable to complete max items enforcer for " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }
}
