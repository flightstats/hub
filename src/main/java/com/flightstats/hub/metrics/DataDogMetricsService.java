package com.flightstats.hub.metrics;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;

public class DataDogMetricsService implements MetricsService {
    private final static StatsDClient statsd = DataDog.statsd;

    @Override
    public void insert(String channelName, Content content, long time) {
        statsd.time("channel", time, "method:post", "type:single", "channel:" + channelName);
        statsd.increment("channel.items", "method:post", "type:single", "channel:" + channelName);
        statsd.count("channel.bytes", content.getSize(), "method:post", "type:single", "channel:" + channelName);

    }

    @Override
    public void historicalInsert(String channelName, Content content, long time) {
        statsd.time("channel.historical", time, "method:post", "type:single", "channel:" + channelName);
        statsd.count("channel.historical.bytes", content.getSize(), "method:post", "type:single", "channel:" + channelName);
    }

    @Override
    public void insert(BulkContent bulkContent, long time) {
        String channel = bulkContent.getChannel();
        statsd.time("channel", time, "method:post", "type:bulk", "channel:" + channel);
        statsd.count("channel.items", bulkContent.getItems().size(), "method:post", "type:bulk", "channel:" + channel);
        statsd.count("channel.bytes", bulkContent.getSize(), "method:post", "type:bulk", "channel:" + channel);
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
        statsd.recordExecutionTime(operationName, System.currentTimeMillis() - start, "channel:" + channel, tag);
    }

    @Override
    public void operation(String channel, String operationName, long start, long bytes, String tag) {
        statsd.recordExecutionTime(operationName, System.currentTimeMillis() - start, "channel:" + channel, tag);
        statsd.count(operationName + ".bytes", bytes, "channel:" + channel, tag);
    }

}
