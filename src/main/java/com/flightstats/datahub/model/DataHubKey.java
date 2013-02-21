package com.flightstats.datahub.model;

import org.joda.time.format.ISODateTimeFormat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

public class DataHubKey {

    private final Date date;
    private final int sequence;

    public DataHubKey(Date date, int sequence) {
        this.date = date;
        this.sequence = sequence;
    }

    public Date getDate() {
        return date;
    }

    public int getSequence() {
        return sequence;
    }

    public String asSortableString() {
        String result = ISODateTimeFormat.dateTime().print(date.getTime());

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(8);
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeLong(date.getTime());
        } catch (IOException e) {
            throw new RuntimeException("Error converting long to bytes", e);
        }

        return result + "." + String.format("%06d", sequence);
    }

    public static DataHubKey fromSortableString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataHubKey that = (DataHubKey) o;

        if (sequence != that.sequence) {
            return false;
        }
        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + sequence;
        return result;
    }

    @Override
    public String toString() {
        return "DataHubKey{" +
                "date=" + date +
                ", sequence=" + sequence +
                '}';
    }
}
