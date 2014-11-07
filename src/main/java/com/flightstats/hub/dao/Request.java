package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

import java.net.URI;
import java.util.Date;

@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"channel", "key"})
public class Request {
    private final String channel;
    private final String user;
    private final URI uri;
    private final ContentKey key;
    private Date date = new Date();

}
