package com.flightstats.hub.dao.aws.s3verifier;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.spoke.SpokeStoreConfig;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;

import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;

public class VerifierRangeLookup {
    private final VerifierConfig verifierConfig;
    private final SpokeStoreConfig spokeWriteStoreConfig;

    private final LastContentPath lastContentPath;
    private final ChannelService channelService;

    @Inject
    public VerifierRangeLookup(LastContentPath lastContentPath,
                               ChannelService channelService,
                               VerifierConfig verifierConfig,
                               @Named("spokeWriteStoreConfig") SpokeStoreConfig spokeWriteStoreConfig) {
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.verifierConfig = verifierConfig;
        this.spokeWriteStoreConfig = spokeWriteStoreConfig;
    }

    public VerifierRange getSingleVerifierRange(DateTime now, ChannelConfig channelConfig) {
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        now = channelService.getLastUpdated(channelConfig.getDisplayName(), new MinutePath(now)).getTime();
        DateTime start = now.minusMinutes(1);
        MinutePath endPath = new MinutePath(start);
        MinutePath defaultStart = new MinutePath(start.minusMinutes(verifierConfig.getOffsetMinutes()));
        MinutePath startPath = (MinutePath) lastContentPath.get(channelConfig.getDisplayName(), defaultStart, LAST_SINGLE_VERIFIED);
        if (channelConfig.isLive() && startPath.compareTo(spokeTtlTime) < 0) {
            startPath = spokeTtlTime;
        }
        return VerifierRange.builder()
                .channelConfig(channelConfig)
                .startPath(startPath)
                .endPath(endPath)
                .build();
    }

    private MinutePath getSpokeTtlPath(DateTime now) {
        return new MinutePath(now.minusMinutes(spokeWriteStoreConfig.getTtlMinutes() - 2));
    }
}
