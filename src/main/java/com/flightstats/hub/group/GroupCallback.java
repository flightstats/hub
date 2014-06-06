package com.flightstats.hub.group;

public interface GroupCallback {
    void notifyWatchers();
    long getLastCompleted(Group group);
}
