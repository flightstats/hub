package com.flightstats.hub.dao.aws.s3verifier;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerifierRange {
    private final MinutePath startPath;
    private final MinutePath endPath;
    private final ChannelConfig channelConfig;
}
