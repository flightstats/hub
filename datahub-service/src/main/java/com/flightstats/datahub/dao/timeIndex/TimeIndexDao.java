package com.flightstats.datahub.dao.timeIndex;

import com.flightstats.datahub.model.ContentKey;
import org.joda.time.DateTime;

import java.util.List;

/**
 *
 */
public interface TimeIndexDao {

    void writeIndex(String channelName, DateTime dateTime, ContentKey key);

    void writeIndices(String channelName, String dateTime, List<String> keys);
}
