package com.flightstats.hub.webhook;

import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryGenerator {

    private final static Logger logger = LoggerFactory.getLogger(QueryGenerator.class);
    private DateTime lastQueryTime;
    private String channel;

    public QueryGenerator(DateTime startTime, String channel) {
        lastQueryTime = startTime;
        this.channel = channel;
    }

    TimeQuery getQuery(DateTime latestStableInChannel) {
        logger.trace("iterating last {} stable {} ", lastQueryTime, latestStableInChannel);
        if (lastQueryTime.isBefore(latestStableInChannel)) {
            TimeUtil.Unit unit = getStepUnit(latestStableInChannel);
            Location location = Location.ALL;
            if (unit.equals(TimeUtil.Unit.SECONDS)) {
                location = Location.CACHE;
            } else if (unit.equals(TimeUtil.Unit.DAYS)) {
                logger.info("long term query unit={} lastQueryTime={}", unit, lastQueryTime);
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
