package com.flightstats.hub.metrics;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;

public interface MetricsService {

    void insert(String channelName, Content content, long time);

    void historicalInsert(String channelName, Content content, long time);

    void insert(BulkContent bulkContent, long time);

    void event(String title, String text, String[] tags);

    void getValue(String channel, long time);
}
