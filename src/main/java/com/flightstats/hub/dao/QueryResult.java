package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;


public class QueryResult {

    private SortedSet<ContentKey> contentKeys = Collections.synchronizedSortedSet(new TreeSet<>());
    private AtomicInteger success = new AtomicInteger();
    private int attempts;

    public QueryResult(int attempts) {
        this.attempts = attempts;
    }

    public void addKeys(Collection<ContentKey> keys) {
        contentKeys.addAll(keys);
        success.incrementAndGet();
    }

    public SortedSet<ContentKey> getContentKeys() {
        return contentKeys;
    }

    public boolean hadSuccess() {
        return success.get() >= 1;
    }

    @Override
    public String toString() {
        return "contentKeys=" + contentKeys.size() + " success=" + success + " attempts=" + attempts;
    }
}
