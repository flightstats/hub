package com.flightstats.hub.model;

import java.io.Serializable;

/**
 *
 */
public interface ContentKey extends Serializable, Comparable<ContentKey> {
    ContentKey getNext();

    ContentKey getPrevious();

    public String keyToString();

}
