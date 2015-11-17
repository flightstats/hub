package com.flightstats.hub.group;

public interface GroupProcessor {

    void notifyWatchers();

    void getStatus(Group group, GroupStatus.GroupStatusBuilder statusBuilder);

    void delete(String name);
}
