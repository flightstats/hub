package com.flightstats.hub.util;

import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;

//todo - gfm - 10/28/14 - probably can go away
public class TimeContentKeyGenerator implements ContentKeyGenerator {
    @Override
    public ContentKey newKey(String channelName) {
        return new ContentKey();
    }

    @Override
    public void seedChannel(String channelName) {
        //todo - gfm - 10/28/14 - do nothing - go away

    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return ContentKey.fromString(keyString);
    }

    @Override
    public void delete(String channelName) {
        //todo - gfm - 10/28/14 - do nothing - go away

    }

    @Override
    public void setLatest(String channelName, ContentKey contentKey) {
        //todo - gfm - 10/28/14 - do nothing - go away
    }
}
