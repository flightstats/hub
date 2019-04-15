package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChannelItem {

    private String timestamp;
    private Links _links;

    @Value
    public static class Links {
        Link self;
        Link channel;
    }

    @Value
    public static class Link {
        String href;
    }
}
