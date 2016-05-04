package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ChannelConfig;
import org.joda.time.DateTime;

public abstract class S3VerifierHandler {
    abstract void singleS3Verification(final DateTime startTime, final ChannelConfig channel, DateTime endTime);
}
