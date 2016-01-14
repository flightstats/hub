package com.flightstats.hub.stream;

import com.flightstats.hub.model.Content;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreamContent {
    private Content content;
    private boolean first;
    private String channel;

}
