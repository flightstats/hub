package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
class SpokeServer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeServer.class);

    private String name;
    private int failureCount = HubProperties.getProperty("spoke.failure.count", 10);
    private int failureMinutes = HubProperties.getProperty("spoke.failure.window.minutes", 1);
    private DateTime startTime = TimeUtil.now();
    private final Set<Failure> failures = Collections.newSetFromMap(new ConcurrentHashMap<Failure, Boolean>());
    private final AtomicBoolean failed = new AtomicBoolean();

    public SpokeServer(String name) {
        this.name = name;
    }

    @VisibleForTesting
    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public boolean isEligible() {
        if (failed.get()) {
            return false;
        }
        if (failures.size() > failureCount) {
            DateTime now = TimeUtil.now();
            DateTime startWindowEnd = startTime.plusMinutes(failureMinutes);
            long count = failures.stream()
                    .filter(failure -> failure.getDateTime().isBefore(startWindowEnd))
                    .count();
            if (count > failureCount) {
                //todo - gfm - 10/12/15 - tell self server to restart itself
                logger.warn("server failed {} should restart itself! {} {}", name, count, failures);
                failed.set(true);
                return false;
            }
        }
        return true;
    }

    public void logFailure(String message) {
        logFailure(message, null);
    }

    public void logFailure(String message, Exception exception) {
        Failure failure = Failure.builder().message(message).exception(exception).build();
        logger.warn("failure {} ", failure);
        failures.add(failure);
    }

    @Override
    public String toString() {
        return name;
    }
}
