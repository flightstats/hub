package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ContentKey;

import java.util.Collection;
import java.util.TreeSet;

class ContentKeySet extends TreeSet<ContentKey> {

    private final int maxSize;
    private final ContentKey limitKey;

    ContentKeySet(int maxSize, ContentKey limitKey) {
        this.maxSize = maxSize;
        this.limitKey = limitKey;
    }

    @Override
    public boolean add(ContentKey contentKey) {
        if (contentKey.compareTo(limitKey) >= 0) {
            return false;
        }
        boolean add = super.add(contentKey);
        if (size() > maxSize) {
            remove(oldest());
        }
        return add;
    }

    @Override
    public boolean addAll(Collection<? extends ContentKey> contentKeys) {
        for (ContentKey contentKey : contentKeys) {
            add(contentKey);
        }
        return true;
    }

    private ContentKey oldest() {
        return first();
    }
}
