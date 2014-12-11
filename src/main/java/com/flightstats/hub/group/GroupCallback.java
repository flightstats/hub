package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;

public interface GroupCallback {
    void notifyWatchers();

    ContentKey getLastCompleted(Group group);
    void delete(String name);
}
