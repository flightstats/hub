package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;

/**
 *
 */
public interface ContentServiceFinder {

    ContentService getContentService(ChannelConfiguration channelConfiguration);
}
