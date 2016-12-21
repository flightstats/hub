package com.flightstats.hub.metrics;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;

public interface MetricsService {

    void insert(String channelName, Content content, long time);

    void historicalInsert(String channelName, Content content, long time);

    void insert(BulkContent bulkContent, long time);

    //todo gfm - change [] to ...
    void event(String title, String text, String[] tags);

    void getValue(String channel, long time);

    void operation(String channel, String operationName, long start, String tag);

    void operation(String channel, String name, long start, long bytes, String tag);
}
