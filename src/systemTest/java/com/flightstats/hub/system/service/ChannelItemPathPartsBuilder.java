package com.flightstats.hub.system.service;

import com.flightstats.hub.model.ChannelItemPathParts;
import com.google.inject.Inject;
import org.joda.time.DateTime;

public class ChannelItemPathPartsBuilder {
    private final String baseUrl;

    @Inject
    public ChannelItemPathPartsBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ChannelItemPathPartExtractor getChannelItemPathPartExtractor(String itemUrl) {
        return new ChannelItemPathPartExtractor(getTrimmedPath(itemUrl));
    }

    public ChannelItemPathParts buildFromItemUrl(String path) {
        ChannelItemPathPartExtractor extractor = getChannelItemPathPartExtractor(path);

        DateTime dateTime = extractor.getDateTime();
        return ChannelItemPathParts.builder()
                .path(extractor.getPath())
                .timePath(extractor.getTimePath())
                .channelName(extractor.getChannelName())
                .hashKey(extractor.getHashKey())
                .dateTime(dateTime)
                .year(dateTime.getYear())
                .month(dateTime.getMonthOfYear())
                .day(dateTime.getDayOfMonth())
                .hour(dateTime.getHourOfDay())
                .minute(dateTime.getMinuteOfHour())
                .second(dateTime.getSecondOfMinute())
                .millis(dateTime.getMillisOfSecond())
                .build();
    }

    private String getTrimmedPath(String path) {
        return path.replace(baseUrl + "channel/", "");
    }
}
