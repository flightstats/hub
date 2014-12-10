package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import lombok.Getter;
import lombok.experimental.Builder;

@Getter
@Builder
public class GroupStatus {
    private ContentKey lastCompleted;
    private ContentKey channelLatest;
    private Group group;

    @JsonIgnore
    public Group getGroup() {
        return group;
    }

    public String getName() {
        return group.getName();
    }

}
