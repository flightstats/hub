package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.google.inject.Inject;

public class HubHealthCheck implements HealthCheck {

	private final ChannelService channelService;

    @Inject
	public HubHealthCheck(ChannelService channelService) {
		this.channelService = channelService;
    }

    @Override
    public boolean isHealthy() {
        return channelService.isHealthy();
    }
}
