package com.flightstats.hub.spoke;

import com.flightstats.hub.config.SpokeProperties;
import com.google.inject.Provider;

import javax.inject.Inject;

public class SpokeWriteStoreConfigProvider implements Provider<SpokeStoreConfig> {

    private final SpokeProperties spokeProperties;

    @Inject
    public SpokeWriteStoreConfigProvider(SpokeProperties spokeProperties) {
        this.spokeProperties = spokeProperties;
    }

    @Override
    public SpokeStoreConfig get() {
        return SpokeStoreConfig.builder()
                .type(SpokeStore.WRITE)
                .path(spokeProperties.getPath(SpokeStore.WRITE))
                .ttlMinutes(spokeProperties.getTtlMinutes(SpokeStore.WRITE))
                .build();
    }
}
