package com.flightstats.hub.filter;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class MetricsFilteringDynamicBinding implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        boolean shouldReport = !resourceInfo.getResourceClass().isAnnotationPresent(SkipMetricsReporting.class);
        if (shouldReport) {
            context.register(MetricsRequestFilter.class);
        }
    }
}
