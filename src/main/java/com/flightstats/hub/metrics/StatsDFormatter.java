package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.util.TimeUtil;
import com.timgroup.statsd.Event;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class StatsDFormatter {

    private final MetricsProperties metricsProperties;

    @Inject
    StatsDFormatter(MetricsProperties metricsProperties){
        this.metricsProperties = metricsProperties;
    }

    Event buildCustomEvent(String title, String text) {

        boolean valid = Stream.of(title, text)
                .allMatch((str) -> str != null && !str.equals(""));
        if (!valid) {
            log.error("Statsd events error: cannot build event without valid text/title strings");
            return Event.builder().build();
        }
        return Event.builder()
                .withHostname(metricsProperties.getHostTag())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING)
                .withTitle(title)
                .withText(text)
                .build();
    }

    long startTimeMillis(long start) {
        return TimeUtil.now().getMillis() - start;
    }

    String[] formatChannelTags(String channel, String... tags) {
        List<String> tagList = Arrays
                .stream(tags)
                .collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[0]);
    }

}
