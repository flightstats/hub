package com.flightstats.hub.group;

import com.google.common.base.Optional;

import java.util.Collection;

public interface GroupDao {
    Group upsertGroup(Group group);

    Optional<Group> getGroup(String name);

    Collection<Group> getGroups();

    void delete(String name);
}
