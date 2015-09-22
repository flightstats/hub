package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GroupStatus {
    private ContentPath lastCompleted;
    private ContentKey channelLatest;
    private Group group;
    private List<String> errors;
    private List<ContentPath> inFlight;

    @JsonIgnore
    public Group getGroup() {
        return group;
    }

    public String getName() {
        return group.getName();
    }

}
