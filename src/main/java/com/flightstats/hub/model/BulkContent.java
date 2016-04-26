package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class BulkContent {

    private final boolean isNew;
    private final InputStream stream;
    private final String contentType;
    private final String channel;
    private final List<Content> items = new ArrayList<>();
    private ContentKey masterKey;

    public void setMasterKey(ContentKey masterKey) {
        this.masterKey = masterKey;
    }

    public long getSize() {
        long bytes = 0;
        for (Content item : items) {
            bytes += item.getSize();
        }
        return bytes;
    }


}
