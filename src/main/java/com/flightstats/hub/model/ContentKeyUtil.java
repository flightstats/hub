package com.flightstats.hub.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class ContentKeyUtil {

    public static void convertKeyStrings(String keysString, Collection<ContentKey> contentKeys) {
        if (StringUtils.isNotEmpty(keysString)) {
            String[] keys = keysString.split(",");
            for (String key : keys) {
                contentKeys.add(ContentKey.fromUrl(StringUtils.substringAfter(key, "/")).get());
            }
        }
    }
}
