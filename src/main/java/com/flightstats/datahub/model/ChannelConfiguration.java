package com.flightstats.datahub.model;

import java.util.Date;

public class ChannelConfiguration {

    private final String name;
    private final String description;
    private final Date creationDate;

    public ChannelConfiguration(String name, String description, Date creationDate) {
        this.creationDate = creationDate;
        this.description = description;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
