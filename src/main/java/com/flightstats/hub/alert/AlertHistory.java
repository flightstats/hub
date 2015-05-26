package com.flightstats.hub.alert;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
//todo - gfm - 5/22/15 - delete
public class AlertHistory {
    private int count;
    private String self;
    private String next;
    private String previous;

    public boolean hasNext() {
        return next != null;
    }

}
