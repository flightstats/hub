package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ContentKey;

import java.util.Collection;
import java.util.TreeSet;

public class ContentKeySet extends TreeSet<ContentKey> {

    private final int maxSize;

    public ContentKeySet(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(ContentKey contentKey) {
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

    public ContentKey oldest() {
        return first();
    }
}
