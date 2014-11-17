package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Date;

public class InsertedContentKey {

    private final Date date;

    public InsertedContentKey(ContentKey key) {
        this.date = new DateTime(key.getMillis()).toDate();
    }

    @JsonProperty("timestamp")
    public Date getDate() {
        return date;
    }

}
