package com.flightstats.hub.cluster;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@ToString
class ClusterEvent {

    private static final Logger logger = LoggerFactory.getLogger(ClusterEvent.class);
    private static final String PIPE = Pattern.quote("|");

    private final String name;
    private final long time;
    private final boolean added;

    ClusterEvent(String event) {
        String[] splitEvent = event.split(PIPE);
        time = Long.parseLong(splitEvent[0]);
        name = splitEvent[1];
        added = "ADDED".equalsIgnoreCase(splitEvent[2]);
    }

    static String encode(String nodeName, long creationTime, boolean added) {
        return creationTime + "|" + nodeName + "|" + (added ? "ADDED" : "REMOVED");
    }
}
