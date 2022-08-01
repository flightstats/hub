package com.flightstats.hub.system;

import com.flightstats.hub.util.StringUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemTestUtils {
    public static String randomChannelName() {
        return randomChannelName(10);
    }

    public static String randomChannelName(int randomLength) {
        return "test_"              // channels that begin with test_ are ignored for metrics
                + "automated_"      // denote it's from an automated system test
                + StringUtils.randomAlphaNumeric(randomLength);
    }
}
