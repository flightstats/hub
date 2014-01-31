package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
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
