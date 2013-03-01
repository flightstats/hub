package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/health")
public class HealthCheck {

    private final ChannelDao channelDao;

    @Inject
    public HealthCheck(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String check() {
        int channelCount = channelDao.countChannels();
        return "OK (" + channelCount + " channels)";
    }

}
