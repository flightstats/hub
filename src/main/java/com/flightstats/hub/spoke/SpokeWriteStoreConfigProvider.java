package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Provider;

public class SpokeWriteStoreConfigProvider implements Provider<SpokeStoreConfig> {
    @Override
    public SpokeStoreConfig get() {
        return SpokeStoreConfig.builder()
                .type(SpokeStore.WRITE)
                .path(HubProperties.getSpokePath(SpokeStore.WRITE))
                .ttlMinutes(HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE))
                .build();
    }
}
