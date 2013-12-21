package com.flightstats.datahub.model;

import com.google.common.base.Optional;

import java.io.Serializable;

/**
 *
 */
public interface DataHubKey extends Serializable {
    Optional<DataHubKey> getNext();

    Optional<DataHubKey> getPrevious();

    public String keyToString();

}
