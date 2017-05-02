package com.flightstats.hub.cluster;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(of = "name")
class SpokeNode {

    private String name;
    private String ipAddress;
    private int port;

    @Override
    public String toString() {
        return name;
    }
}
