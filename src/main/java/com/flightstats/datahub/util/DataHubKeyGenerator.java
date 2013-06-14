package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;

public interface DataHubKeyGenerator {

	DataHubKey newKey(String channelName);
}
