package com.flightstats.hub.model;

import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode(of = {"data", "contentType", "contentLanguage", "user"})
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

    public int getDataLength() {
        return data == null ? 0 : data.length;
    }

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = Optional.of(contentKey);
    }

    /**
     * @return true if this Content is new, false if it has been inserted elsewhere and is a replicant.
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
}
