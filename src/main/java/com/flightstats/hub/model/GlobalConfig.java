package com.flightstats.hub.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;
import java.util.TreeSet;

@ToString
@EqualsAndHashCode()
@Getter
//todo - gfm - 5/19/16 - could be builder
public class GlobalConfig {

    @Setter
    private String master;
    private Set<String> satellites = new TreeSet<>();

    //todo - gfm - 5/19/16 - this could live elsewhere
    @Setter
    private boolean isMaster = false;

}
