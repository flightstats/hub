package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Getter;

@Builder()
@Getter
public class RegulatedConfig {

    @Builder.Default
    private int percentUtilization = 90;
    @Builder.Default
    private int maxThreads = 20;
    @Builder.Default
    private int startThreads = 2;
    @Builder.Default
    private TimeUtil.Unit timeUnit = TimeUtil.Unit.MINUTES;
    @Builder.Default
    private int timeValue = 1;
    private String name;
}
