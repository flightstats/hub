package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Links {
    private Link self;
    private Link channel;
    private String [] uris;
}
