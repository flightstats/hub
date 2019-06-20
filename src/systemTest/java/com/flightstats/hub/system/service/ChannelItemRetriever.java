package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.Links;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQueryResult;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import org.joda.time.DateTime;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ChannelItemRetriever {
    private final ChannelItemResourceClient channelItemResourceClient;
    private final HttpUrl hubBaseUrl;

    @Inject
    public ChannelItemRetriever(HubClientFactory hubClientFactory) {
        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    public String getChannelUrl(String channelName) {
        return getHubBaseUrl() + "channel/" + channelName;
    }

    private HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
    }
    @SneakyThrows
    private Optional<TimeQueryResult> getItemByTimeFromLocation(String path, Location location) {
        FormattedStringHelper helper = new FormattedStringHelper().withExtractedUrlPaths(path, hubBaseUrl.toString());
        Call<TimeQueryResult> response = channelItemResourceClient.getItemsSecondsPath(
                helper.getChannelName(),
                helper.getYear(),
                helper.getMonth(),
                helper.getDay(),
                helper.getHour(),
                helper.getMinute(),
                helper.getSecond(),
                location);
        Optional<TimeQueryResult> op = Optional.ofNullable(response.execute().body());
        op.filter(o -> o.get_links() != null);
        return op;
    }

    @SneakyThrows
    public Optional<Object> getItem(String path) {
        FormattedStringHelper helper = new FormattedStringHelper().withExtractedUrlPaths(path, hubBaseUrl.toString());
        Object response = channelItemResourceClient.get(
                helper.getChannelName(),
                helper.getYear(),
                helper.getMonth(),
                helper.getDay(),
                helper.getHour(),
                helper.getMinute(),
                helper.getSecond(),
                helper.getMillis(),
                helper.getHashKey());
        return Optional.ofNullable(((Call) response).execute().body());
    }

    public boolean confirmItemInCache(String itemUri, Location location) {
        TimeQueryResult result = getItemByTimeFromLocation(itemUri, location)
                .orElse(TimeQueryResult.builder()._links(Links.builder().uris(new String[]{}).build()).build());
        List<String> uris = Arrays.asList(result.get_links().getUris());
        return uris.stream().anyMatch(str -> str.equals(itemUri));
    }
}