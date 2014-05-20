package com.flightstats.hub.dao;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

import java.util.Date;

@Builder
@Getter
@ToString
public class Request {
    private final String channel;
    private final String user;
    private final String uri;
    private final String id;
    private Date date = new Date();

}
