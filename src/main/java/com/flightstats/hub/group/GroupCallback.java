package com.flightstats.hub.group;

public interface GroupCallback {
    void notifyWatchers();
    void buildStatus(Group group, GroupStatus.GroupStatusBuilder builder);
    void delete(String name);
}
