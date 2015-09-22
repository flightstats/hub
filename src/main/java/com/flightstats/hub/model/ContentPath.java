package com.flightstats.hub.model;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ContentPath extends Comparable<ContentPath> {
    Logger logger = LoggerFactory.getLogger(ContentPath.class);

    byte[] toBytes();

    ContentPath toContentPath(byte[] bytes);

    String toUrl();

    DateTime getTime();

    String toZk();

    ContentPath fromZk(String value);

    static Optional<ContentPath> fromFullUrl(String url) {
        try {
            String substring = StringUtils.substringAfter(url, "/channel/");
            substring = StringUtils.substringAfter(substring, "/");
            Optional<ContentKey> keyOptional = ContentKey.fromUrl(substring);
            if (keyOptional.isPresent()) {
                return Optional.of(keyOptional.get());
            }
            Optional<MinutePath> pathOptional = MinutePath.fromUrl(substring);
            if (pathOptional.isPresent()) {
                return Optional.of(pathOptional.get());
            }
            return Optional.absent();
        } catch (Exception e) {
            logger.info("unable to parse " + url + " " + e.getMessage());
            return Optional.absent();
        }

    }

}
