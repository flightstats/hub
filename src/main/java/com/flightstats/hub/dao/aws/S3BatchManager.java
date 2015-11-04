package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class S3BatchManager {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchManager.class);

    private final ChannelService channelService;
    private final HubUtils hubUtils;

    @Inject
    public S3BatchManager(ChannelService channelService, HubUtils hubUtils) {
        this.channelService = channelService;
        this.hubUtils = hubUtils;
        HubServices.registerPreStop(new S3BatchManagerService());
    }

    private class S3BatchManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            Executors.newSingleThreadExecutor().submit(S3BatchManager.this::setupBatch);
        }

        @Override
        protected void shutDown() throws Exception {
        }

    }

    private void setupBatch() {
        for (ChannelConfig channel : channelService.getChannels()) {
            S3Batch s3Batch = new S3Batch(channel, hubUtils);
            if (channel.isSingle()) {
                logger.debug("turning off batch {}", channel.getName());
                s3Batch.stop();
            } else {
                logger.info("batching channel {}", channel.getName());
                s3Batch.start();
            }
        }
    }

}
