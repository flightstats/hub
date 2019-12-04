package com.flightstats.hub.model;


import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InternalProperties {
    private String server;
    private List<String> servers;
}
