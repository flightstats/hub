package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.experimental.Builder;

@Getter
@Builder
public class GroupStatus {
    private long lastCompleted;
    private long channelLatest;
    private Group group;

    @JsonIgnore
    public Group getGroup() {
        return group;
    }

    public String getName() {
        return group.getName();
    }

    public String getChannel() {
        return group.getChannelUrl();
    }
}
