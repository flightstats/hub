package com.flightstats.datahub.util;

import com.flightstats.datahub.model.ContentKey;
import com.google.common.base.Optional;

public interface DataHubKeyGenerator {

    ContentKey newKey(String channelName);

    void seedChannel(String channelName);

    Optional<ContentKey> parse(String keyString);
}
