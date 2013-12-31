package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;

/**
 *
 */
public class SingleContentServiceFinder implements ContentServiceFinder {
    private final ContentService contentService;

    @Inject
    public SingleContentServiceFinder(ContentService contentService) {
        this.contentService = contentService;
    }

    @Override
    public ContentService getContentService(ChannelConfiguration channelConfiguration) {
        return contentService;
    }
}
