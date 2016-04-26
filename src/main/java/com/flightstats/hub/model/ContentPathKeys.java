package com.flightstats.hub.model;

import java.util.Collection;

public interface ContentPathKeys extends ContentPath {
    Collection<ContentKey> getKeys();
}
