package com.flightstats.hub.model;

import org.joda.time.DateTime;

public interface Query {

    DateTime getChannelStable();

    Location getLocation();

    Epoch getEpoch();

    String getChannelName();

    ChannelConfig getChannelConfig();

    boolean outsideOfCache(DateTime cacheTime);

    String getUrlPath();

    boolean isStable();
}
