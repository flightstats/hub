package com.flightstats.hub.dao.timeIndex;

import java.util.List;

/**
 *
 */
public interface TimeIndexDao {

    void writeIndices(String channelName, String dateTime, List<String> keys);
}
