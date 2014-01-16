package com.flightstats.datahub.model;

import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.Arrays;

public class Content implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    private final Optional<String> contentLanguage;
    private final long millis;
    private final byte[] data;
    private Optional<ContentKey> contentKey = Optional.absent();

    public Content(Optional<String> contentType, Optional<String> contentLanguage, byte[] data, long millis) {
        this.contentType = contentType;
        this.contentLanguage = contentLanguage;
        this.millis = millis;
        this.data = data;
    }

    public Content(Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        this(contentType, contentLanguage, data, System.currentTimeMillis());
    }

    public Optional<String> getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return data == null ? 0 : data.length;
    }

    public Optional<String> getContentLanguage() {
        return contentLanguage;
    }

    public long getMillis() {
        return millis;
    }

    public Optional<ContentKey> getContentKey() {
        return contentKey;
    }

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = Optional.of(contentKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Content that = (Content) o;

        if (!contentLanguage.equals(that.contentLanguage)) {
            return false;
        }
        if (!contentType.equals(that.contentType)) {
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
        result = 31 * result + (contentLanguage != null ? contentLanguage.hashCode() : 0);
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        return result;
    }
}
