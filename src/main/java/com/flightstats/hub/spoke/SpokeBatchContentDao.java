package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

public class SpokeBatchContentDao extends SpokeContentDao {

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return insertIntoStore("batch", channelName, content);
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        return getFromStore("batch", channelName, key);
    }

}
