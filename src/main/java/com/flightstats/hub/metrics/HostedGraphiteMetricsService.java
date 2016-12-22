package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;

public class HostedGraphiteMetricsService implements MetricsService {

    private final static MetricsSender sender = HubProvider.getInstance(MetricsSender.class);

    @Override
    public void insert(String channelName, Content content, long time) {
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".items", 1);
        sender.send("channel." + channelName + ".post.bytes", content.getSize());
        sender.send("channel.ALL.post", time);
    }

    @Override
    public void historicalInsert(String channelName, Content content, long time) {
        insert(channelName, content, time);
    }

    @Override
    public void insert(BulkContent bulkContent, long time) {
        String channel = bulkContent.getChannel();
        sender.send("channel." + channel + ".batchPost", time);
        sender.send("channel." + channel + ".items", bulkContent.getItems().size());
        sender.send("channel." + channel + ".post", time);
        sender.send("channel." + channel + ".post.bytes", bulkContent.getSize());
        sender.send("channel.ALL.post", time);
    }

    @Override
    public void event(String title, String text, String[] tags) {
        //do nothing
    }

    @Override
    public void getValue(String channel, long time) {
        sender.send("channel." + channel + ".get", time);
    }

    @Override
    public void operation(String channel, String operationName, long start, String tag) {
        sender.send("channel." + channel + "." + operationName, 1);
    }

    @Override
    public void operation(String channel, String operationName, long start, long bytes, String tag) {
        sender.send("channel." + channel + "." + operationName, 1);
        sender.send("channel." + channel + "." + operationName + ".bytes", bytes);
    }
}
