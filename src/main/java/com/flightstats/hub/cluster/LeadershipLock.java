package com.flightstats.hub.cluster;

import lombok.Builder;
import lombok.Value;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

@Value
@Builder
public class LeadershipLock {
    String lockPath;
    InterProcessSemaphoreMutex mutex;
    Leadership leadership;
}
