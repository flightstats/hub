package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.HealthStatus;
import com.google.inject.Inject;

import java.util.concurrent.atomic.AtomicBoolean;

public class HubHealthCheckImpl implements HubHealthCheck {

	private final ChannelService channelService;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    @Inject
	public HubHealthCheckImpl(ChannelService channelService) {
		this.channelService = channelService;
    }

    @Override
    public HealthStatus getStatus() {
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder();
        if (shutdown.get()) {
            return builder.healthy(false).description("SHUTTING DOWN").build();
        }
        return builder.healthy(true).description("OK").build();
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
    }

}
