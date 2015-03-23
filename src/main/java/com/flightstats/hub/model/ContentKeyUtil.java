package com.flightstats.hub.model;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class ContentKeyUtil {

    public static void convertKeyStrings(String keysString, Collection<ContentKey> contentKeys) {
        if (StringUtils.isNotEmpty(keysString)) {
            String[] keys = keysString.split(",");
            for (String key : keys) {
                contentKeys.add(convertKey(key).get());
            }
        }
    }

    public static Optional<ContentKey> convertKey(String key) {
        if (StringUtils.isNotEmpty(key)) {
            return ContentKey.fromUrl(StringUtils.substringAfter(key, "/"));
        }
        return Optional.absent();
    }
}
