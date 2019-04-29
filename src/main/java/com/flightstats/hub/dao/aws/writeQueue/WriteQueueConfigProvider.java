package com.flightstats.hub.dao.aws.writeQueue;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Provider;

public class WriteQueueConfigProvider implements Provider<WriteQueueConfig> {
    @Override public WriteQueueConfig get() {
        return WriteQueueConfig.builder()
                .queueSize(HubProperties.getProperty("s3.writeQueueSize", 40000))
                .threads(HubProperties.getProperty("s3.writeQueueThreads", 20))
                .build();
    }
}
