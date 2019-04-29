package com.flightstats.hub.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

class ClusterEvent {

    private static final Logger logger = LoggerFactory.getLogger(ClusterEvent.class);
    private static final String PIPE = Pattern.quote("|");
    private static final String SLASH = Pattern.quote("/");

    private final String name;
    private final long creationTime;
    private final boolean added;
    private final long modifiedTime;
    private String event;

    ClusterEvent(String event, long modifiedTime) {
        this.event = event;
        this.modifiedTime = modifiedTime;
        String[] splitEvent = event.split(PIPE);
        creationTime = Long.parseLong(splitEvent[0].split(SLASH)[2]);
        name = splitEvent[1];
        added = "ADDED".equalsIgnoreCase(splitEvent[2]);
    }

    static String encode(String name, long creationTime, boolean added) {
        return creationTime + "|" + name + "|" + (added ? "ADDED" : "REMOVED");
    }

    static Set<ClusterEvent> set() {
        return new TreeSet<>(comparator());
    }

    private static Comparator<ClusterEvent> comparator() {
        return (first, second) -> {
            long diff = first.getModifiedTime() - second.getModifiedTime();
            if (diff == 0) {
                diff = first.getCreationTime() - second.getCreationTime();
            }
            if (diff == 0) {
                diff = Boolean.compare(first.isAdded(), second.isAdded());
            }
            if (diff == 0) {
                diff = first.getName().compareTo(second.getName());
            }
            return (int) diff;
        };
    }

    String encode() {
        return encode(name, creationTime, added);
    }

    public String event() {
        return event;
    }

    public String getName() {
        return this.name;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public boolean isAdded() {
        return this.added;
    }

    public String getEvent() {
        return this.event;
    }

    public long getModifiedTime() {
        return this.modifiedTime;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ClusterEvent)) return false;
        final ClusterEvent other = (ClusterEvent) o;
        if (!other.canEqual(this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        if (this.getCreationTime() != other.getCreationTime()) return false;
        if (this.isAdded() != other.isAdded()) return false;
        final Object this$event = this.getEvent();
        final Object other$event = other.getEvent();
        if (this$event == null ? other$event != null : !this$event.equals(other$event)) return false;
        return this.getModifiedTime() == other.getModifiedTime();
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final long $creationTime = this.getCreationTime();
        result = result * PRIME + (int) ($creationTime >>> 32 ^ $creationTime);
        result = result * PRIME + (this.isAdded() ? 79 : 97);
        final Object $event = this.getEvent();
        result = result * PRIME + ($event == null ? 43 : $event.hashCode());
        final long $modifiedTime = this.getModifiedTime();
        result = result * PRIME + (int) ($modifiedTime >>> 32 ^ $modifiedTime);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClusterEvent;
    }

    public String toString() {
        return "com.flightstats.hub.cluster.ClusterEvent(name=" + this.getName() + ", creationTime=" + this.getCreationTime() + ", added=" + this.isAdded() + ", event=" + this.getEvent() + ", modifiedTime=" + this.getModifiedTime() + ")";
    }
}
