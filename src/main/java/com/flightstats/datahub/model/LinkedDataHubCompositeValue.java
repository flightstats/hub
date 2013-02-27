package com.flightstats.datahub.model;

import com.google.common.base.Optional;

public class LinkedDataHubCompositeValue {

    private final DataHubCompositeValue value;
    private final Optional<DataHubKey> previous;

    public LinkedDataHubCompositeValue(DataHubCompositeValue value, Optional<DataHubKey> previous) {
        this.value = value;
        this.previous = previous;
    }

    public String getContentType() {
        return value.getContentType();
    }

    public int getDataLength() {
        return value.getDataLength();
    }

    public int getContentTypeLength() {
        return value.getContentTypeLength();
    }

    public byte[] getData() {
        return value.getData();
    }

    public boolean hasPrevious() {
        return previous.isPresent();
    }

    public Optional<DataHubKey> getPrevious() {
        return previous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LinkedDataHubCompositeValue that = (LinkedDataHubCompositeValue) o;

        if (previous != null ? !previous.equals(that.previous) : that.previous != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (previous != null ? previous.hashCode() : 0);
        return result;
    }
}
