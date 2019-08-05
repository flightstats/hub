package com.flightstats.hub.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;


@Slf4j
public class LogbackFilter extends Filter<ILoggingEvent> {

    private boolean filterAwsGet404(ILoggingEvent event) {
        String message = event.getMessage();
        return StringUtils.isNotBlank(message) &&
                Stream.of("AmazonS3Exception", "GET", "Status Code: 404")
                .allMatch(message::contains);
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (filterAwsGet404(event)) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }
}
