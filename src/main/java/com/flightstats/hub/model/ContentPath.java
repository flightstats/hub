package com.flightstats.hub.model;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
            return Optional.empty();
        }
    }

    static Optional<ContentPath> fromUrl(String url) {
        Optional<ContentKey> keyOptional = ContentKey.fromUrl(url);
        if (keyOptional.isPresent()) {
            return Optional.of(keyOptional.get());
        }
        Optional<MinutePath> pathOptional = MinutePath.fromUrl(url);
        if (pathOptional.isPresent()) {
            return Optional.of(pathOptional.get());
        }
        Optional<SecondPath> secondPathOptional = SecondPath.fromUrl(url);
        if (secondPathOptional.isPresent()) {
            return Optional.of(secondPathOptional.get());
        }
        return Optional.empty();
    }

}
