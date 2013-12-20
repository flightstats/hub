package com.flightstats.datahub.dao;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class TimedDataHubValueDao implements DataHubValueDao {
    public static final String DELEGATE = "TimedDataHubValueDao.DELEGATE";
    private final DataHubValueDao delegate;
    private final MetricsTimer metricsTimer;

    @Inject
    public TimedDataHubValueDao(@Named(DELEGATE) DataHubValueDao delegate, MetricsTimer metricsTimer) {
        this.delegate = delegate;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public ValueInsertionResult write(final String channelName, final DataHubCompositeValue columnValue, final Optional<Integer> ttlSeconds) {
        return metricsTimer.time("valueDao.write", new TimedCallback<ValueInsertionResult>() {
            @Override
            public ValueInsertionResult call() {
                return delegate.write(channelName, columnValue, ttlSeconds);
            }
        });
    }

    @Override
    public DataHubCompositeValue read(final String channelName, final DataHubKey key) {
        return metricsTimer.time("valueDao.read", new TimedCallback<DataHubCompositeValue>() {
            @Override
            public DataHubCompositeValue call() {
                return delegate.read(channelName, key);
            }
        });
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public void initializeChannel(final String channelName) {
        metricsTimer.time("valueDao.initializeChannel", new TimedCallback<Object>() {
            @Override
            public Object call() {
                delegate.initializeChannel(channelName);
                return null;
            }
        });
    }
}
