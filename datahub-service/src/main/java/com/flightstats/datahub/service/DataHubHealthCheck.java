package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.google.inject.Inject;

public class DataHubHealthCheck implements HealthCheck {

	private final ChannelService channelService;

	@Inject
	public DataHubHealthCheck(ChannelService channelService) {
		this.channelService = channelService;
	}

    @Override
    public boolean isHealthy() {
        //todo - gfm - 12/19/13 - this should check if zookeeper has a connection/quorum.
        return channelService.isHealthy();
    }
}
