package com.flightstats.hub.alert;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AlertHistory {
    private int count;
    private String self;
    private String next;
    private String previous;

}
