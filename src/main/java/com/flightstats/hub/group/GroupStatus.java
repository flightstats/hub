package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GroupStatus {
    private ContentKey lastCompleted;
    private ContentKey channelLatest;
    private Group group;
    private List<String> errors;
    private List<ContentKey> inFlight;

    @JsonIgnore
    public Group getGroup() {
        return group;
    }

    public String getName() {
        return group.getName();
    }

}
