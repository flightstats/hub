package com.flightstats.hub.webhook.strategy;

import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

@Slf4j
public class QueryGenerator {
    private DateTime lastQueryTime;
    private String channel;

    public QueryGenerator(DateTime startTime, String channel) {
        lastQueryTime = startTime;
        this.channel = channel;
    }

    TimeQuery getQuery(DateTime latestStableInChannel) {
        log.trace("iterating last {} stable {} ", lastQueryTime, latestStableInChannel);
        if (lastQueryTime.isBefore(latestStableInChannel)) {
            TimeUtil.Unit unit = getStepUnit(latestStableInChannel);
            Location location = Location.ALL;
            if (unit.equals(TimeUtil.Unit.SECONDS)) {
                location = Location.CACHE_WRITE;
            } else if (unit.equals(TimeUtil.Unit.DAYS)) {
                log.info("long term query unit={} lastQueryTime={}", unit, lastQueryTime);
            }
            TimeQuery query = TimeQuery.builder()
                    .channelName(channel)
                    .startTime(lastQueryTime)
                    .unit(unit)
                    .location(location)
                    .epoch(Epoch.IMMUTABLE)
                    .build();
            lastQueryTime = unit.round(lastQueryTime.plus(unit.getDuration()));
            return query;
        } else {
            return null;
        }
    }

    private TimeUtil.Unit getStepUnit(DateTime latestStableInChannel) {
        if (lastQueryTime.isBefore(latestStableInChannel.minusHours(2))) {
            return TimeUtil.Unit.HOURS;
        } else if (lastQueryTime.isBefore(latestStableInChannel.minusMinutes(2))) {
            return TimeUtil.Unit.MINUTES;
        }
        return TimeUtil.Unit.SECONDS;
    }

    DateTime getLastQueryTime() {
        return lastQueryTime;
    }
}
