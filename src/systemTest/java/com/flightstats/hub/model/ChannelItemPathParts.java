package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import lombok.Builder;
import lombok.Value;
import okhttp3.HttpUrl;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

@Value
@Builder
public class ChannelItemPathParts {
    String itemUrl;
    String path;

    public String getChannelName() {
        return getPath().substring(0, getPath().indexOf("/"));
    }

    public String getHashKey() {
        return getPath().substring(getPath().lastIndexOf("/") + 1);
    }

    public String getTimePath() {
        return getPath()
                .replace(getChannelName(), "")
                .replace(getHashKey(), "")
                .substring(1);
    }

    public String getZookeeperItemPath() {
        return getPath()
                .replace(getChannelName(), "")
                .substring(1);
    }

    public String getSecondUrl() {
        String overlyGranularPathParts = format("%03d/%s", getMillis(), getHashKey());
        return getItemUrl().substring(0, getItemUrl().indexOf(overlyGranularPathParts) - 1);
    }

    public DateTime getDateTime() {
        return TimeUtil.millis(getTimePath());
    }

    public Integer getYear() {
        return getUnitOfTime(DateTime::getYear);
    }

    public Integer getMonth() {
        return getUnitOfTime(DateTime::getMonthOfYear);
    }

    public Integer getDay() {
        return getUnitOfTime(DateTime::getDayOfMonth);
    }

    public Integer getHour() {
        return getUnitOfTime(DateTime::getHourOfDay);
    }

    public Integer getMinute() {
        return getUnitOfTime(DateTime::getMinuteOfHour);
    }

    public Integer getSecond() {
        return getUnitOfTime(DateTime::getSecondOfMinute);
    }

    public Integer getMillis() {
        return getUnitOfTime(DateTime::getMillisOfSecond);
    }

    public static class ChannelItemPathPartsBuilder {
        private HttpUrl baseUrl;

        public ChannelItemPathPartsBuilder baseUrl(HttpUrl baseUrl) {
            this.baseUrl = baseUrl;
            setPath();
            return this;
        }

        public ChannelItemPathPartsBuilder itemUrl(String itemUrl) {
            this.itemUrl = itemUrl;
            setPath();
            return this;
        }

        private void setPath() {
            if (baseUrl != null && itemUrl != null) {
                this.path = itemUrl.replace(baseUrl + "channel/", "");
            }
        }
    }

    private Integer getUnitOfTime(Function<DateTime, Integer> getUnit) {
        return Optional.ofNullable(getDateTime())
                .map(getUnit)
                .orElse(null);
    }
}
