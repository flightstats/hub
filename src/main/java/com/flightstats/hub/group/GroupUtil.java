package com.flightstats.hub.group;

import org.apache.commons.lang3.StringUtils;

public class GroupUtil {

    public static String getChannelName(Group group) {
        return StringUtils.substringAfter(group.getChannelUrl(), "/channel/");
    }
}
