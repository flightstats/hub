package com.flightstats.datahub.model;

import java.util.Date;

public class ChannelConfiguration {

    private final String name;
    private final Date creationDate;
    private final DataHubKey lastUpdateKey;

    public ChannelConfiguration(String name, Date creationDate, DataHubKey lastUpdateKey) {
        this.creationDate = creationDate;
        this.name = name;
        this.lastUpdateKey = lastUpdateKey;
    }

    public String getName() {
        return name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public DataHubKey getLastUpdateKey() {
        return lastUpdateKey;
    }

    public Date getLastUpdateDate() {
        return lastUpdateKey.getDate();
    }

    public ChannelConfiguration updateLastUpdateKey(DataHubKey latestKey) {
        return new ChannelConfiguration(name, creationDate, latestKey);
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

        if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) {
            return false;
        }
        if (lastUpdateKey != null ? !lastUpdateKey.equals(that.lastUpdateKey) : that.lastUpdateKey != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (lastUpdateKey != null ? lastUpdateKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChannelConfiguration{" +
                "name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdateKey=" + lastUpdateKey +
                '}';
    }
}
