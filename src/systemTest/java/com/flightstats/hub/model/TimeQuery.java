package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class TimeQuery {
    private final Links _links;

    public String [] getUris() {
        return this._links.uris;
    }

    @Value
    public static class Links {
        private String [] uris;
    }

}
