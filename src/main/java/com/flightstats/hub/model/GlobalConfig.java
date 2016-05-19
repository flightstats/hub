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
public class GlobalConfig {

    @Setter
    private String master;
    private Set<String> satellites = new TreeSet<>();

}
