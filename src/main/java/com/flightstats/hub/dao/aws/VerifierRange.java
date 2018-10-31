package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
class VerifierRange {
    private final MinutePath startPath;
    private final MinutePath endPath;
    private final ChannelConfig channelConfig;
}
