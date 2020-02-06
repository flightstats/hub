package com.flightstats.hub.model;

import java.util.Collection;

public interface DurationBasedContentPath extends ContentPath {
    Collection<ContentKey> getKeys();
}
