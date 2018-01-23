package com.flightstats.hub.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class RegulatorStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RegulatorStrategy.class);

    static RegulatorResults calculate(ExecutorState state) {
        RegulatorResults.RegulatorResultsBuilder builder = RegulatorResults.builder()
                .threads(state.getThreads())
                .sleepTime(state.getSleep());
        if (state.getThreads() == 1) {
            if (state.getRatio() >= 1) {
                long sleepTime = state.getSize() * state.getSleep();
                long runTime = state.getEnd() - state.getStart() - sleepTime;
                if (runTime > state.getGoalMillis()) {
                    setThreads(state, builder, (double) runTime / state.getGoalMillis());
                } else {
                    builder.sleepTime((state.getGoalMillis() - runTime) / state.getSize());
                }
            } else {
                long difference = state.getGoalMillis() - (state.getEnd() - state.getStart());
                long additionalSleep = difference / state.getSize();
                builder.sleepTime(state.getSleep() + additionalSleep);
            }
        } else {
            setThreads(state, builder, state.getRatio());
        }
        RegulatorResults results = builder.build();
        for (Map.Entry<String, Long> entry : state.getResults().entrySet()) {
            if (entry.getValue() > state.getGoalMillis()) {
                results.getSlowChannels().put(entry.getKey(), entry.getValue());
                logger.info("adding slow channel {} {}", entry.getKey(), entry.getValue());
            }
        }
        logger.info("results {}", results);
        return results;
    }

    private static void setThreads(ExecutorState state, RegulatorResults.RegulatorResultsBuilder builder, double ratio) {
        int threads = (int) Math.ceil(ratio * state.getThreads());
        builder.threads(threads);
        builder.sleepTime(0);
    }
}
