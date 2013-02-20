package com.flightstats.datahub.model;

import java.util.Arrays;

public class DataHubCompositeValue {

    private final String contentType;
    private final byte[] data;

    public DataHubCompositeValue(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public int getContentTypeLength() {
        return contentType == null ? 0 : contentType.length();
    }

    public int getDataLength() {
        return data == null ? 0 : data.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataHubCompositeValue that = (DataHubCompositeValue) o;

        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) {
            return false;
        }
        if (!Arrays.equals(data, that.data)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        return result;
    }
}
