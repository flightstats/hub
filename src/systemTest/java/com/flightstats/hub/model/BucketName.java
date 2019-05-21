package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BucketName {
    private static final String PREFIX = "hub_";
    private static final String POSTFIX = "-local";
    private String releaseName;

    public String getFullName() {
        return PREFIX + releaseName + POSTFIX;
    }
}
