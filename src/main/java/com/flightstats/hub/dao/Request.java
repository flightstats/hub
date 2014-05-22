package com.flightstats.hub.dao;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

import java.net.URI;
import java.util.Date;

@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"channel", "id"})
public class Request {
    private final String channel;
    private final String user;
    private final URI uri;
    private final String id;
    private Date date = new Date();

}
