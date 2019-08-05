package com.flightstats.hub.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LogbackFilter extends Filter<ILoggingEvent> {

    private boolean filterAwsGet404(ILoggingEvent event) {
        try {
            log.info("&&&&&&&&&&&& {}", event.getMDCPropertyMap());
            return event.getLevel().toString().contains("WARN") &&
                    event.getMessage().contains("com.flightstats.hub.dao.aws.AwsConnectorFactory") &&
                    event.getMessage().contains("Status Code: 404");
        } catch (Exception e) {
            return false;
        }

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
