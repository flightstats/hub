package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.experimental.Builder;

import java.util.ArrayList;

@Getter
@Builder
public class GroupStatus {
    private long lastCompleted;
    private long channelLatest;
    private Group group;
    private Iterable<Long> inProcess = new ArrayList<>();

    @JsonIgnore
    public Group getGroup() {
        return group;
    }

    public String getName() {
        return group.getName();
    }

}
