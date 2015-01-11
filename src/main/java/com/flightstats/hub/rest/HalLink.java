package com.flightstats.hub.rest;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.net.URI;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class HalLink {
    private final String name;
    private final URI uri;
}

