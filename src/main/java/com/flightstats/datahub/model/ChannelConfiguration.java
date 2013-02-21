package com.flightstats.datahub.model;

import java.util.Date;

public class ChannelConfiguration {

    private final String name;
    private final Date creationDate;
    private final Date lastUpdateDate;

    public ChannelConfiguration(String name, Date creationDate, Date lastUpdateDate) {
        this.creationDate = creationDate;
        this.name = name;
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getName() {
        return name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public ChannelConfiguration updateLastUpdateDate(Date date) {
        return new ChannelConfiguration(name, creationDate, date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChannelConfiguration that = (ChannelConfiguration) o;

        if (!creationDate.equals(that.creationDate)) {
            return false;
        }
        if (lastUpdateDate != null ? !lastUpdateDate.equals(that.lastUpdateDate) : that.lastUpdateDate != null) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + creationDate.hashCode();
        result = 31 * result + (lastUpdateDate != null ? lastUpdateDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChannelConfiguration{" +
                "name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdateDate=" + lastUpdateDate +
                '}';
    }
}
