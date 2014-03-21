package com.flightstats.hub.util;

import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;

public interface ContentKeyGenerator {

    ContentKey newKey(String channelName);

    void seedChannel(String channelName);

    Optional<ContentKey> parse(String keyString);

    void delete(String channelName);
}
