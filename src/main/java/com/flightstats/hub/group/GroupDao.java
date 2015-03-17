package com.flightstats.hub.group;

import com.google.common.base.Optional;

public interface GroupDao {
    Group upsertGroup(Group group);

    Optional<Group> getGroup(String name);

    Iterable<Group> getGroups();

    void delete(String name);

    String getTableName();
}
