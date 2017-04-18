package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

/**
 * If an endTime or limitKey is specified, the TimeQuery will move forward
 * by TimeUtil.Unit increments until the endTime or limitKey is reached.
 */
public class TimeQuery implements Query {
    private final String channelName;
    private final ChannelConfig channelConfig;
    private final String tagName;
    private final DateTime startTime;
    private final TimeUtil.Unit unit;
    private final Location location;
    private final boolean stable;
    private final int count;
    private final ContentKey limitKey;
    private final Epoch epoch;
    private final DateTime channelStable;

    @java.beans.ConstructorProperties({"channelName", "channelConfig", "tagName", "startTime", "unit", "location", "stable", "count", "limitKey", "epoch", "channelStable"})
    private TimeQuery(String channelName, ChannelConfig channelConfig, String tagName, DateTime startTime, TimeUtil.Unit unit, Location location, boolean stable, int count, ContentKey limitKey, Epoch epoch, DateTime channelStable) {
        this.channelName = channelName;
        this.channelConfig = channelConfig;
        this.tagName = tagName;
        this.startTime = startTime;
        this.unit = unit;
        this.location = location;
        this.stable = stable;
        this.count = count;
        this.limitKey = limitKey;
        this.epoch = epoch;
        this.channelStable = channelStable;
    }

    public static TimeQueryBuilder builder() {
        return new TimeQueryBuilder();
    }

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    public Epoch getEpoch() {
        if (epoch == null) {
            return Epoch.IMMUTABLE;
        }
        return epoch;
    }

    public boolean outsideOfCache(DateTime cacheTime) {
        return startTime.isBefore(cacheTime);
    }

    public String getUrlPath() {
        return "/" + getUnit().format(getStartTime()) + "?stable=" + stable;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public ChannelConfig getChannelConfig() {
        return this.channelConfig;
    }

    public String getTagName() {
        return this.tagName;
    }

    public DateTime getStartTime() {
        return this.startTime;
    }

    public TimeUtil.Unit getUnit() {
        return this.unit;
    }

    public boolean isStable() {
        return this.stable;
    }

    public int getCount() {
        return this.count;
    }

    public ContentKey getLimitKey() {
        return this.limitKey;
    }

    public DateTime getChannelStable() {
        return this.channelStable;
    }

    public String toString() {
        return "com.flightstats.hub.model.TimeQuery(channelName=" + this.getChannelName() + ", channelConfig=" + this.getChannelConfig() + ", tagName=" + this.getTagName() + ", startTime=" + this.getStartTime() + ", unit=" + this.getUnit() + ", location=" + this.getLocation() + ", stable=" + this.isStable() + ", count=" + this.getCount() + ", limitKey=" + this.getLimitKey() + ", epoch=" + this.getEpoch() + ", channelStable=" + this.getChannelStable() + ")";
    }

    public TimeQuery withChannelName(String channelName) {
        return this.channelName == channelName ? this : new TimeQuery(channelName, this.channelConfig, this.tagName, this.startTime, this.unit, this.location, this.stable, this.count, this.limitKey, this.epoch, this.channelStable);
    }

    public TimeQuery withChannelConfig(ChannelConfig channelConfig) {
        return this.channelConfig == channelConfig ? this : new TimeQuery(this.channelName, channelConfig, this.tagName, this.startTime, this.unit, this.location, this.stable, this.count, this.limitKey, this.epoch, this.channelStable);
    }

    public TimeQuery withStartTime(DateTime startTime) {
        return this.startTime == startTime ? this : new TimeQuery(this.channelName, this.channelConfig, this.tagName, startTime, this.unit, this.location, this.stable, this.count, this.limitKey, this.epoch, this.channelStable);
    }

    public TimeQuery withLocation(Location location) {
        return this.location == location ? this : new TimeQuery(this.channelName, this.channelConfig, this.tagName, this.startTime, this.unit, location, this.stable, this.count, this.limitKey, this.epoch, this.channelStable);
    }

    public TimeQuery withEpoch(Epoch epoch) {
        return this.epoch == epoch ? this : new TimeQuery(this.channelName, this.channelConfig, this.tagName, this.startTime, this.unit, this.location, this.stable, this.count, this.limitKey, epoch, this.channelStable);
    }

    public TimeQuery withChannelStable(DateTime channelStable) {
        return this.channelStable == channelStable ? this : new TimeQuery(this.channelName, this.channelConfig, this.tagName, this.startTime, this.unit, this.location, this.stable, this.count, this.limitKey, this.epoch, channelStable);
    }

    public static class TimeQueryBuilder {
        private String channelName;
        private ChannelConfig channelConfig;
        private String tagName;
        private DateTime startTime;
        private TimeUtil.Unit unit;
        private Location location;
        private boolean stable;
        private int count;
        private ContentKey limitKey;
        private Epoch epoch;
        private DateTime channelStable;

        TimeQueryBuilder() {
        }

        public TimeQuery.TimeQueryBuilder channelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public TimeQuery.TimeQueryBuilder channelConfig(ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public TimeQuery.TimeQueryBuilder tagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public TimeQuery.TimeQueryBuilder startTime(DateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public TimeQuery.TimeQueryBuilder unit(TimeUtil.Unit unit) {
            this.unit = unit;
            return this;
        }

        public TimeQuery.TimeQueryBuilder location(Location location) {
            this.location = location;
            return this;
        }

        public TimeQuery.TimeQueryBuilder stable(boolean stable) {
            this.stable = stable;
            return this;
        }

        public TimeQuery.TimeQueryBuilder count(int count) {
            this.count = count;
            return this;
        }

        public TimeQuery.TimeQueryBuilder limitKey(ContentKey limitKey) {
            this.limitKey = limitKey;
            return this;
        }

        public TimeQuery.TimeQueryBuilder epoch(Epoch epoch) {
            this.epoch = epoch;
            return this;
        }

        public TimeQuery.TimeQueryBuilder channelStable(DateTime channelStable) {
            this.channelStable = channelStable;
            return this;
        }

        public TimeQuery build() {
            return new TimeQuery(channelName, channelConfig, tagName, startTime, unit, location, stable, count, limitKey, epoch, channelStable);
        }

        public String toString() {
            return "com.flightstats.hub.model.TimeQuery.TimeQueryBuilder(channelName=" + this.channelName + ", channelConfig=" + this.channelConfig + ", tagName=" + this.tagName + ", startTime=" + this.startTime + ", unit=" + this.unit + ", location=" + this.location + ", stable=" + this.stable + ", count=" + this.count + ", limitKey=" + this.limitKey + ", epoch=" + this.epoch + ", channelStable=" + this.channelStable + ")";
        }
    }
}
