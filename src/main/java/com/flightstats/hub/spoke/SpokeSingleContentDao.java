package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

public class SpokeSingleContentDao extends SpokeContentDao {

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return insertIntoStore("single", channelName, content);
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        return getFromStore("single", channelName, key);
    }

}
