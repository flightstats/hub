package com.flightstats.hub.metrics;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;

public class NoOpMetricsService implements MetricsService {
    @Override
    public void insert(String channelName, Content content, long time) {

    }

    @Override
    public void historicalInsert(String channelName, Content content, long time) {

    }

    @Override
    public void insert(BulkContent bulkContent, long time) {

    }

    @Override
    public void event(String title, String text, String[] tags) {

    }

    @Override
    public void getValue(String channel, long time) {

    }

    @Override
    public void operation(String channel, String operationName, long start, String tag) {

    }

    @Override
    public void operation(String channel, String name, long start, long bytes, String tag) {

    }
}
