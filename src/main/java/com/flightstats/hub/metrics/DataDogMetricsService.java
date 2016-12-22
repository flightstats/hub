package com.flightstats.hub.metrics;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;

public class DataDogMetricsService implements MetricsService {
    private final static StatsDClient statsd = DataDog.statsd;

    @Override
    public void insert(String channel, Content content, long time) {
        doInsert(channel, time, "single", 1, content.getSize());
    }

    @Override
    public void historicalInsert(String channelName, Content content, long time) {
        doInsert(channelName, time, "historical", 1, content.getSize());
    }

    @Override
    public void insert(BulkContent bulkContent, long time) {
        doInsert(bulkContent.getChannel(), time, "bulk", bulkContent.getItems().size(), bulkContent.getSize());
    }

    private void doInsert(String channel, long time, String type, int items, Long bytes) {
        operation(channel, "channel", time, bytes, "type:" + type);
        statsd.count("channel.items", items, "method:post", "type:" + type, "channel:" + channel);
    }

    @Override
    public void event(String title, String text, String[] tags) {
        Event event = DataDog.getEventBuilder()
                .withTitle(title)
                .withText(text)
                .build();
        DataDog.statsd.recordEvent(event, tags);
    }

    @Override
    public void getValue(String channel, long time) {
        //todo gfm - I'm not sure we need this for DD
        statsd.time("channel", time, "channel:" + channel, "method:get");
    }

    @Override
    public void operation(String channel, String operationName, long start, String tag) {
        statsd.time(operationName, System.currentTimeMillis() - start, "channel:" + channel, tag);
    }

    @Override
    public void operation(String channel, String operationName, long start, long bytes, String tag) {
        statsd.time(operationName, System.currentTimeMillis() - start, "channel:" + channel, tag);
        statsd.count(operationName + ".bytes", bytes, "channel:" + channel, tag);
    }

}
