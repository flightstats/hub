package com.flightstats.datahub.model;

import com.google.common.base.Optional;

/**
 *
 */
public class TimeSeriesDataHubKey implements DataHubKey {


    @Override
    public Optional<DataHubKey> getNext() {
        return Optional.absent();
    }

    @Override
    public Optional<DataHubKey> getPrevious() {
        return Optional.absent();
    }

    @Override
    public String keyToString() {
        return null;
    }
}
