package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

/**
 *
 */
public interface ContentServiceFinder {

    ContentService getContentService(ChannelConfiguration channelConfiguration);
}
