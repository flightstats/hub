package com.flightstats.hub.model;

import org.joda.time.DateTime;

public interface Query {

    Location getLocation();

    String getChannelName();

    boolean outsideOfCache(DateTime cacheTime);

    String getUrlPath();
}
