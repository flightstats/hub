package com.flightstats.hub.cluster;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@ToString
class ClusterEvent {

    private static final Logger logger = LoggerFactory.getLogger(ClusterEvent.class);
    private static final String PIPE = Pattern.quote("|");
    private static final String SLASH = Pattern.quote("/");

    private final String name;
    private final long creationTime;
    private final boolean added;
    private String event;
    private final long modifiedTime;

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

    String encode() {
        return encode(name, creationTime, added);
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

    public String event() {
        return event;
    }
}
