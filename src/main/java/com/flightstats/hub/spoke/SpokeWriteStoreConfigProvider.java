package com.flightstats.hub.spoke;

import com.flightstats.hub.config.SpokeProperty;
import com.google.inject.Provider;

import javax.inject.Inject;

public class SpokeWriteStoreConfigProvider implements Provider<SpokeStoreConfig> {

    private SpokeProperty spokeProperty;

    @Inject
    public SpokeWriteStoreConfigProvider(SpokeProperty spokeProperty) {
        this.spokeProperty = spokeProperty;
    }

    @Override
    public SpokeStoreConfig get() {
        return SpokeStoreConfig.builder()
                .type(SpokeStore.WRITE)
                .path(spokeProperty.getPath(SpokeStore.WRITE))
                .ttlMinutes(spokeProperty.getTtlMinutes(SpokeStore.WRITE))
                .build();
    }
}
