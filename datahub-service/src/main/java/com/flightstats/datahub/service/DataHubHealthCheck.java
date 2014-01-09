package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ZooKeeperState;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.google.inject.Inject;

//todo - gfm - 1/9/14 - see if we can mod this to use 404 vs 500
public class DataHubHealthCheck implements HealthCheck {

	private final ChannelService channelService;
    private final ZooKeeperState zooKeeperState;

    @Inject
	public DataHubHealthCheck(ChannelService channelService, ZooKeeperState zooKeeperState) {
		this.channelService = channelService;
        this.zooKeeperState = zooKeeperState;
    }

    @Override
    public boolean isHealthy() {
        return channelService.isHealthy() && zooKeeperState.isHealthy();
    }
}
