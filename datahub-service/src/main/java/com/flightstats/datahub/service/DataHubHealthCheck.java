package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class DataHubHealthCheck implements HealthCheck {

	private final ChannelDao channelDao;

	@Inject
	public DataHubHealthCheck(ChannelDao channelDao) {
		this.channelDao = channelDao;
	}

    @Override
    public boolean isHealthy() {
        // Don't care about the result, just verify a trip to the persistence layer doesn't explode.
        channelDao.countChannels();
        return true;
    }
}
