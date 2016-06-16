package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class DataDog {
    public final static StatsDClient statsd = HubProperties.getProperty("data_dog.enable", false) ?
            new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"})
            : new NoOpStatsD();

    @Inject
    public DataDog() {
    }

    public static List<String> addTag(List<String> tags, String tagName, String value) {
        if (tags == null) {
            tags = new ArrayList<String>();
        }
        tags.add(tagName + ":" + value);
        return tags;
    }

    public static String[] tagsAsArray(List<String> tags) {
        return tags.toArray(new String[tags.size()]);
    }

}

