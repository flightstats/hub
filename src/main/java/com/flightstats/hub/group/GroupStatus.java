package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import lombok.Getter;
import lombok.experimental.Builder;

import java.util.List;

@Getter
@Builder
public class GroupStatus {
    private String name;
    private ContentKey lastCompleted;
    private ContentKey channelLatest;
    private List<String> errors;
    private List<ContentKey> inFlight;

}
