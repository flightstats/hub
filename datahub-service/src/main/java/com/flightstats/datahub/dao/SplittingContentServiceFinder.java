package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;

/**
 *
 */
public class SplittingContentServiceFinder implements ContentServiceFinder {

    private final ContentService sequentialService;
    private final ContentService timeSeriesService;

    @Inject
    public SplittingContentServiceFinder(@Sequential ContentService sequentialService,
                                   @TimeSeries ContentService timeSeriesService) {
        this.sequentialService = sequentialService;
        this.timeSeriesService = timeSeriesService;
    }

    @Override
    public ContentService getContentService(ChannelConfiguration channelConfiguration) {
        if (channelConfiguration.isSequence()) {
            return sequentialService;
        }
        return timeSeriesService;
    }
}
