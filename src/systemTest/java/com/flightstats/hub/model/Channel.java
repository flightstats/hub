package com.flightstats.hub.model;

import lombok.Builder;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Set;

@Builder
public class Channel {

    private String name;
    private String displayName;
    private boolean keepForever;
    private String owner;
    private Date creationDate;
    private long ttlDays;
    private long maxItems;
    private String description;
    private Set<String> tags;
    private String replicationSource;
    private String storage;
    private boolean protect;
    private DateTime mutableTime;
    private boolean allowZeroBytes;
}
