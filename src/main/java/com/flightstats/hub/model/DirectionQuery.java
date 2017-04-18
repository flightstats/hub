package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

public class DirectionQuery implements Query {
    private final String channelName;
    private final ChannelConfig channelConfig;
    private final String tagName;
    /**
     * The startKey is exclusive.
     */
    private ContentKey startKey;
    private final int count;
    private final boolean next;
    private final Location location;
    private final boolean stable;

    /**
     * earliestTime is only relevant for previous queries.
     */
    private DateTime earliestTime;

    private final DateTime channelStable;

    private final Epoch epoch;

    @java.beans.ConstructorProperties({"channelName", "channelConfig", "tagName", "startKey", "count", "next", "location", "stable", "earliestTime", "channelStable", "epoch"})
    private DirectionQuery(String channelName, ChannelConfig channelConfig, String tagName, ContentKey startKey, int count, boolean next, Location location, boolean stable, DateTime earliestTime, DateTime channelStable, Epoch epoch) {
        this.channelName = channelName;
        this.channelConfig = channelConfig;
        this.tagName = tagName;
        this.startKey = startKey;
        this.count = count;
        this.next = next;
        this.location = location;
        this.stable = stable;
        this.earliestTime = earliestTime;
        this.channelStable = channelStable;
        this.epoch = epoch;
    }

    public static DirectionQueryBuilder builder() {
        return new DirectionQueryBuilder();
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

    @Override
    public boolean outsideOfCache(DateTime cacheTime) {
        return !next || startKey.getTime().isBefore(cacheTime);
    }

    @Override
    public String getUrlPath() {
        String direction = next ? "/next/" : "/previous/";
        return "/" + startKey.toUrl() + direction + count + "?stable=" + stable;
    }

    public TimeQuery.TimeQueryBuilder convert(TimeUtil.Unit unit) {
        return TimeQuery.builder().channelName(getChannelName())
                .startTime(startKey.getTime())
                .unit(unit)
                .limitKey(startKey)
                .count(count)
                .epoch(epoch);
    }

    public TimeQuery convert(DateTime startTime, TimeUtil.Unit unit) {
        return convert(unit).startTime(startTime).build();
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

    public ContentKey getStartKey() {
        return this.startKey;
    }

    public int getCount() {
        return this.count;
    }

    public boolean isNext() {
        return this.next;
    }

    public boolean isStable() {
        return this.stable;
    }

    public DateTime getEarliestTime() {
        return this.earliestTime;
    }

    public DateTime getChannelStable() {
        return this.channelStable;
    }

    public String toString() {
        return "com.flightstats.hub.model.DirectionQuery(channelName=" + this.getChannelName() + ", channelConfig=" + this.getChannelConfig() + ", tagName=" + this.getTagName() + ", startKey=" + this.getStartKey() + ", count=" + this.getCount() + ", next=" + this.isNext() + ", location=" + this.getLocation() + ", stable=" + this.isStable() + ", earliestTime=" + this.getEarliestTime() + ", channelStable=" + this.getChannelStable() + ", epoch=" + this.getEpoch() + ")";
    }

    public DirectionQuery withChannelName(String channelName) {
        return this.channelName == channelName ? this : new DirectionQuery(channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withChannelConfig(ChannelConfig channelConfig) {
        return this.channelConfig == channelConfig ? this : new DirectionQuery(this.channelName, channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withTagName(String tagName) {
        return this.tagName == tagName ? this : new DirectionQuery(this.channelName, this.channelConfig, tagName, this.startKey, this.count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withStartKey(ContentKey startKey) {
        return this.startKey == startKey ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, startKey, this.count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withCount(int count) {
        return this.count == count ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withNext(boolean next) {
        return this.next == next ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, next, this.location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withLocation(Location location) {
        return this.location == location ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, location, this.stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withStable(boolean stable) {
        return this.stable == stable ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, stable, this.earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withEarliestTime(DateTime earliestTime) {
        return this.earliestTime == earliestTime ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, this.stable, earliestTime, this.channelStable, this.epoch);
    }

    public DirectionQuery withChannelStable(DateTime channelStable) {
        return this.channelStable == channelStable ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, this.stable, this.earliestTime, channelStable, this.epoch);
    }

    public DirectionQuery withEpoch(Epoch epoch) {
        return this.epoch == epoch ? this : new DirectionQuery(this.channelName, this.channelConfig, this.tagName, this.startKey, this.count, this.next, this.location, this.stable, this.earliestTime, this.channelStable, epoch);
    }

    public static class DirectionQueryBuilder {
        private String channelName;
        private ChannelConfig channelConfig;
        private String tagName;
        private ContentKey startKey;
        private int count;
        private boolean next;
        private Location location;
        private boolean stable;
        private DateTime earliestTime;
        private DateTime channelStable;
        private Epoch epoch;

        DirectionQueryBuilder() {
        }

        public DirectionQuery.DirectionQueryBuilder channelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder channelConfig(ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder tagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder startKey(ContentKey startKey) {
            this.startKey = startKey;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder count(int count) {
            this.count = count;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder next(boolean next) {
            this.next = next;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder location(Location location) {
            this.location = location;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder stable(boolean stable) {
            this.stable = stable;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder earliestTime(DateTime earliestTime) {
            this.earliestTime = earliestTime;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder channelStable(DateTime channelStable) {
            this.channelStable = channelStable;
            return this;
        }

        public DirectionQuery.DirectionQueryBuilder epoch(Epoch epoch) {
            this.epoch = epoch;
            return this;
        }

        public DirectionQuery build() {
            return new DirectionQuery(channelName, channelConfig, tagName, startKey, count, next, location, stable, earliestTime, channelStable, epoch);
        }

        public String toString() {
            return "com.flightstats.hub.model.DirectionQuery.DirectionQueryBuilder(channelName=" + this.channelName + ", channelConfig=" + this.channelConfig + ", tagName=" + this.tagName + ", startKey=" + this.startKey + ", count=" + this.count + ", next=" + this.next + ", location=" + this.location + ", stable=" + this.stable + ", earliestTime=" + this.earliestTime + ", channelStable=" + this.channelStable + ", epoch=" + this.epoch + ")";
        }
    }
}
