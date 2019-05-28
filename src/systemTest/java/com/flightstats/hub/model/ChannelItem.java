package com.flightstats.hub.model;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChannelItem {
    private String timestamp;
    private Links _links;
}
