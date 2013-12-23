package com.flightstats.datahub.model;

import com.google.common.base.Optional;

import java.io.Serializable;

/**
 *
 */
public interface ContentKey extends Serializable {
    Optional<ContentKey> getNext();

    Optional<ContentKey> getPrevious();

    public String keyToString();

}
