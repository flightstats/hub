package com.flightstats.hub.system.service;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

public class ChannelItemPathPartExtractor {
    private final String path;

    ChannelItemPathPartExtractor(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getChannelName() {
        return getPath().substring(0, getPath().indexOf("/"));
    }

    public String getHashKey() {
        return getPath().substring(getPath().lastIndexOf("/") + 1);
    }

    public String getTimePath() {
        return getPath()
                .replace(getChannelName(), "")
                .replace(getHashKey(), "")
                .substring(1);
    }

    public DateTime getDateTime() {
        return TimeUtil.millis(getTimePath());
    }
}
