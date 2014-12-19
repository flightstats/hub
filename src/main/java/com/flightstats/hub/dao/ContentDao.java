package com.flightstats.hub.dao;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Set;

public interface ContentDao {

    String CACHE = "Cache";
    String LONG_TERM = "LongTerm";

    ContentKey write(String channelName, Content content);

    Content read(String channelName, ContentKey key);

    Set<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit);

    Set<ContentKey> query(DirectionQuery query);

    void delete(String channelName);

    void initialize();
}
