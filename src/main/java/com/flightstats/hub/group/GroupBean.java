package com.flightstats.hub.group;

import lombok.Getter;
import lombok.experimental.Builder;

@Getter
@Builder
public class GroupBean {
    private final Iterable<Group> groups;
    private final Iterable<GroupStatus> status;

}
