package com.flightstats.hub.model;

import org.joda.time.DateTime;

public interface ContentPath extends Comparable<ContentPath> {

    byte[] toBytes();

    ContentPath toContentPath(byte[] bytes);

    String toUrl();

    DateTime getTime();

    String toZk();

    ContentKey fromZk(String value);

}
