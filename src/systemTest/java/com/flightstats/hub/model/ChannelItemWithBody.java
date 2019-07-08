package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChannelItemWithBody {
    String url;
    Object body;
}
