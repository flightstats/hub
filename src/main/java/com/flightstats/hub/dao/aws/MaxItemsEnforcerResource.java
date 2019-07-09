package com.flightstats.hub.dao.aws;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.PATCH;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.Optional;

import static javax.ws.rs.core.Response.ok;

@Slf4j
@Path("/internal/max-items")
public class MaxItemsEnforcerResource {

    private final Dao<ChannelConfig> channelConfigDao;
    private final S3Config s3Config;

    @Inject
    public MaxItemsEnforcerResource(S3Config s3Config,
                                    @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao) {
        this.channelConfigDao = channelConfigDao;
        this.s3Config = s3Config;
    }

    @PATCH
    @Path("/{channel}")
    public Response enforce(@PathParam("channel") String channel) {
        try {
            Optional<ChannelConfig> channelConfig = Optional.ofNullable(channelConfigDao.get(channel));
            if (channelConfig.isPresent()) {
                ChannelConfig config = channelConfig.get();
                if (config.getMaxItems() < 1 || config.getKeepForever()) {
                    throw new Exception("channel not configured for max items enforcer");
                }
                S3Config.S3ConfigLockable s3ConfigLockable = s3Config.new S3ConfigLockable(Collections.singletonList(config));
                s3ConfigLockable.updateMaxItems();
                return ok().build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            log.warn("unable to complete max items enforcer for " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }

}
