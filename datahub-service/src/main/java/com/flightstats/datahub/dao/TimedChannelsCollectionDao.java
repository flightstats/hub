package com.flightstats.datahub.dao;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class TimedChannelsCollectionDao implements ChannelsCollectionDao {

    public static final String DELEGATE = "TimedChannelsCollectionDao.DELEGATE";
    private ChannelsCollectionDao delegate;
    private final MetricsTimer metricsTimer;

    @Inject
    public TimedChannelsCollectionDao(@Named(DELEGATE) ChannelsCollectionDao delegate, MetricsTimer metricsTimer) {
        this.delegate = delegate;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public ChannelConfiguration createChannel(final String name, final Long ttlMillis) {
        return metricsTimer.time("channelsCollection.createChannel", new TimedCallback<ChannelConfiguration>() {
            @Override
            public ChannelConfiguration call() {
                return delegate.createChannel(name, ttlMillis);
            }
        });
    }

    @Override
    public void updateChannel(final ChannelConfiguration newConfig) {
        metricsTimer.time("valueDao.updateChannel", new TimedCallback<Object>() {
            @Override
            public Object call() {
                delegate.updateChannel(newConfig);
                return null;
            }
        });
    }

    @Override
    public boolean isHealthy() {
        return metricsTimer.time("channelsCollection.isHealthy", new TimedCallback<Boolean>() {
            @Override
            public Boolean call() {
                return delegate.isHealthy();
            }
        });
    }

    @Override
    public void initializeMetadata() {
        delegate.initializeMetadata();
    }

    @Override
    public boolean channelExists(final String channelName) {
        return metricsTimer.time("channelsCollection.channelExists", new TimedCallback<Boolean>() {
            @Override
            public Boolean call() {
                return delegate.channelExists(channelName);
            }
        });
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(final String channelName) {
        return metricsTimer.time("channelsCollection.getChannelConfiguration", new TimedCallback<ChannelConfiguration>() {
            @Override
            public ChannelConfiguration call() {
                return delegate.getChannelConfiguration(channelName);
            }
        });
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return metricsTimer.time("channelsCollection.getChannels", new TimedCallback<Iterable<ChannelConfiguration>>() {
            @Override
            public Iterable<ChannelConfiguration> call() {
                return delegate.getChannels();
            }
        });
    }
}
