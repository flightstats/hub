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
        time(channel, "channel", time, bytes, "type:" + type);
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
    public void count(String name, long value, String... tag) {
        statsd.count(name, value, tag);
    }

    @Override
    public void time(String channel, String name, long start, String tag) {
        statsd.time(name, System.currentTimeMillis() - start, "channel:" + channel, tag);
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String tag) {
        statsd.time(name, System.currentTimeMillis() - start, "channel:" + channel, tag);
        statsd.count(name + ".bytes", bytes, "channel:" + channel, tag);
    }

}
