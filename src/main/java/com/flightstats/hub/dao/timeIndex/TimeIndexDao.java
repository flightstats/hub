package com.flightstats.hub.dao.timeIndex;

import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;

import java.util.List;

/**
 *
 */
public interface TimeIndexDao {

    void writeIndex(String channelName, DateTime dateTime, ContentKey key);

    void writeIndices(String channelName, String dateTime, List<String> keys);
}
