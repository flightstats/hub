package com.flightstats.hub.metrics;

import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.timgroup.statsd.Event;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class StatsDFormatter {
    private MetricsConfig metricsConfig;

    @Inject
    StatsDFormatter (MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    Event buildCustomEvent(String title, String text) {

        boolean valid = Stream.of(title, text)
                .allMatch((str) -> str != null && !str.equals(""));
        if (!valid) {
            log.error("Statsd events error: cannot build event without valid text/title strings");
            return Event.builder().build();
        }
        return Event.builder()
                .withHostname(metricsConfig.getHostTag())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING)
                .withTitle(title)
                .withText(text)
                .build();
    }

    long startTimeMillis(long start) {
        return TimeUtil.now().getMillis() - start;
    }

    String [] formatChannelTags(String channel, String... tags) {
        List<String> tagList = Arrays
                .stream(tags)
                .collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[0]);
    }

}
