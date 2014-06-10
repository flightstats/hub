package com.flightstats.hub.group;

public interface GroupCallback {
    void notifyWatchers();
    long getLastCompleted(Group group);
    void delete(String name);
}
