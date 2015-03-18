package com.flightstats.hub.group;

import com.google.common.base.Optional;

public class NasGroupDao implements GroupDao {
    @Override
    public Group upsertGroup(Group group) {
        return null;
    }

    @Override
    public Optional<Group> getGroup(String name) {
        return null;
    }

    @Override
    public Iterable<Group> getGroups() {
        return null;
    }

    @Override
    public void delete(String name) {

    }

}
