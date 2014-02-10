package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class InsertedContentKey {

    private final ContentKey key;
    private final Date date;

    public InsertedContentKey(ContentKey key, Date date) {
        this.key = key;
        this.date = date;
    }

    @JsonIgnore
    public ContentKey getKey() {
        return key;
    }

    @JsonProperty("timestamp")
    public Date getDate() {
        return date;
    }

}
