package com.flightstats.datahub.dao.memory;

import com.flightstats.datahub.model.DataHubKey;

import java.util.Date;

//todo - gfm - 11/4/13 - only used by InMemoryChannelDao
class DataHubChannelValueKey {
    private final Date date;
    private final long sequence;
    private final String channelName;

    DataHubChannelValueKey(DataHubKey dataHubKey, String channelName) {
        //todo - gfm - 11/4/13 - what date should this use?
        this.date = dataHubKey.getDate();
        this.sequence = dataHubKey.getSequence();
        this.channelName = channelName;
    }

    long getSequence() {
        return sequence;
    }

    DataHubKey asDataHubKey() {
        return new DataHubKey(date, sequence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataHubChannelValueKey that = (DataHubChannelValueKey) o;

        if (sequence != that.sequence) return false;
        if (!channelName.equals(that.channelName)) return false;
        if (!date.equals(that.date)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
        result = 31 * result + channelName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataHubChannelValueKey{" +
                "date=" + date +
                ", sequence=" + sequence +
                ", channelName='" + channelName + '\'' +
                '}';
    }
}
