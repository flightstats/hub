package com.flightstats.hub.alert;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class AlertStatusHistory {

    private String href;
    private int items;

    private transient String next;
    private transient String previous;

}
