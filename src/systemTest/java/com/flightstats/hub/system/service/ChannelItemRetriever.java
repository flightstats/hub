package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItemPathParts;
import com.flightstats.hub.model.ChannelItemQueryDirection;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQueryResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
public class ChannelItemRetriever {
    private final ChannelItemResourceClient channelItemResourceClient;
    private final HttpUrl hubBaseUrl;

    @Inject
    public ChannelItemRetriever(HubClientFactory hubClientFactory) {
        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    @SneakyThrows
    public Optional<Object> getItem(String path) {
        ChannelItemPathParts pathParts = ChannelItemPathParts.builder()
                .itemUrl(path)
                .baseUrl(hubBaseUrl)
                .build();
        Object response = channelItemResourceClient.get(
                pathParts.getChannelName(),
                pathParts.getYear(),
                pathParts.getMonth(),
                pathParts.getDay(),
                pathParts.getHour(),
                pathParts.getMinute(),
                pathParts.getSecond(),
                pathParts.getMillis(),
                pathParts.getHashKey());
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    public Optional<TimeQueryResult> getItemsForDayLocation(String path, Location location) {
        ChannelItemPathParts pathParts = ChannelItemPathParts.builder()
                .itemUrl(path)
                .baseUrl(hubBaseUrl)
                .build();
        Call<TimeQueryResult> response = channelItemResourceClient.getItemForTimeFromLocation(
                pathParts.getChannelName(),
                pathParts.getYear(),
                pathParts.getMonth(),
                pathParts.getDay(),
                location);
        return Optional.ofNullable(response.execute().body());
    }

    @SneakyThrows
    public Optional<TimeQueryResult> getDirectionalItems(String itemUrl, ChannelItemQueryDirection direction, int numberOfItems) {
        ChannelItemPathParts pathParts = ChannelItemPathParts.builder()
                .itemUrl(itemUrl)
                .baseUrl(hubBaseUrl)
                .build();
        Call<TimeQueryResult> response = channelItemResourceClient.getDirectionalItems(
                pathParts.getPath(),
                direction,
                numberOfItems);
        Optional<TimeQueryResult> op = Optional.ofNullable(response.execute().body());
        op.filter(o -> o.get_links() != null);
        return op;
    }

    @SneakyThrows
    public Optional<Object> getDirectionalItem(String itemUrl, ChannelItemQueryDirection direction) {
        ChannelItemPathParts pathParts = ChannelItemPathParts.builder()
                .itemUrl(itemUrl)
                .baseUrl(hubBaseUrl)
                .build();
        Object response = channelItemResourceClient.getDirectionalItem(pathParts.getPath(), direction);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    public Optional<Object> getEarliestItem(String channelName) {
        Object response = channelItemResourceClient.getDirectionalItem(channelName, ChannelItemQueryDirection.earliest);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    public Optional<Object> getLatestItem(String channelName) {
        Object response = channelItemResourceClient.getDirectionalItem(channelName, ChannelItemQueryDirection.latest);
        return Optional.ofNullable(((Call) response).execute().body());
    }

}
