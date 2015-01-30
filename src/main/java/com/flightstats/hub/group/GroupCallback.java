package com.flightstats.hub.group;

public interface GroupCallback {

    void notifyWatchers();

    void getStatus(Group group, GroupStatus.GroupStatusBuilder statusBuilder);

    void delete(String name);
}
