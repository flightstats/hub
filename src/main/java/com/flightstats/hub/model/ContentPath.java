package com.flightstats.hub.model;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ContentPath extends Comparable<ContentPath> {
    Logger logger = LoggerFactory.getLogger(ContentPath.class);

    byte[] toBytes();

    String toUrl();

    DateTime getTime();

    String toZk();

    ContentPath fromZk(String value);

    int compareTo(ContentPath other);

    static Optional<ContentPath> fromFullUrl(String url) {
        try {
            String substring = StringUtils.substringAfter(url, "/channel/");
            substring = StringUtils.substringAfter(substring, "/");
            return fromUrl(substring);
        } catch (Exception e) {
            logger.info("unable to parse " + url + " " + e.getMessage());
            return Optional.absent();
        }
    }

    //todo - gfm - 10/5/15 - rename
    static Optional<ContentPath> fromUrl(String url) {
        Optional<ContentKey> keyOptional = ContentKey.fromUrl(url);
        if (keyOptional.isPresent()) {
            return Optional.of(keyOptional.get());
        }
        Optional<MinutePath> pathOptional = MinutePath.fromUrl(url);
        if (pathOptional.isPresent()) {
            return Optional.of(pathOptional.get());
        }
        return Optional.absent();
    }

}
