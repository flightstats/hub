package com.flightstats.hub.model;

import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.Arrays;

public class Content implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    private final Optional<String> contentLanguage;
    private final long millis;
    private final byte[] data;
    private final Optional<String> user;
    private Optional<ContentKey> contentKey = Optional.absent();
    private final boolean isNew;

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        isNew = !getContentKey().isPresent();
        contentLanguage = builder.contentLanguage;
        contentType = builder.contentType;
        millis = builder.millis;
        data = builder.data;
        user = builder.user;
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

    public Optional<String> getUser() {
        return user;
    }

    /**
     * @return true if this Content is new, false if it has been inserted elsewhere and is a replica.
     */
    public boolean isNewContent() {
        return isNew;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Optional<String> contentType = Optional.absent();
        private Optional<String> contentLanguage = Optional.absent();
        private long millis = System.currentTimeMillis();
        private Optional<ContentKey> contentKey = Optional.absent();
        private byte[] data;
        private Optional<String> user = Optional.absent();

        public Builder withData(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = Optional.fromNullable(contentType);
            return this;
        }

        public Builder withContentLanguage(String contentLanguage) {
            this.contentLanguage = Optional.fromNullable(contentLanguage);
            return this;
        }

        public Builder withMillis(long millis) {
            this.millis = millis;
            return this;
        }

        public Builder withContentKey(ContentKey contentKey) {
            this.contentKey = Optional.fromNullable(contentKey);
            return this;
        }

        public Builder withUser(String user) {
            this.user = Optional.fromNullable(user);
            return this;
        }

        public Content build() {
            return new Content(this);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Content content = (Content) o;

        if (millis != content.millis) return false;
        if (!contentKey.equals(content.contentKey)) return false;
        if (!contentLanguage.equals(content.contentLanguage)) return false;
        if (!contentType.equals(content.contentType)) return false;
        if (!Arrays.equals(data, content.data)) return false;
        if (!user.equals(content.user)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contentType.hashCode();
        result = 31 * result + contentLanguage.hashCode();
        result = 31 * result + (int) (millis ^ (millis >>> 32));
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        result = 31 * result + user.hashCode();
        result = 31 * result + contentKey.hashCode();
        return result;
    }
}
