package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItemPathParts;
import com.flightstats.hub.model.ChannelItemQueryDirection;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.model.Links;
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
    private final ChannelItemPathPartsBuilder pathPartsBuilder;
    private final HttpUrl hubBaseUrl;

    @Inject
    public ChannelItemRetriever(HubClientFactory hubClientFactory) {
        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
        this.pathPartsBuilder = new ChannelItemPathPartsBuilder(this.hubBaseUrl.toString());
    }

    @SneakyThrows
    public Optional<Object> getItem(String path) {
        ChannelItemPathParts pathParts = pathPartsBuilder.buildFromItemUrl(path);
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
    public Optional<TimeQueryResult> getDirectionalItems(String itemUrl, ChannelItemQueryDirection direction, int numberOfItems) {
        ChannelItemPathParts pathParts = pathPartsBuilder.buildFromItemUrl(itemUrl);
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
        ChannelItemPathParts pathParts = pathPartsBuilder.buildFromItemUrl(itemUrl);
        Object response = channelItemResourceClient.getDirectionalItem(pathParts.getPath(), direction);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    public Optional<Object> getEarliestItem(String channelName) {
        String path = pathPartsBuilder.getChannelItemPathPartExtractor(getChannelUrl(channelName)).getPath();
        Object response = channelItemResourceClient.getDirectionalItem(path, ChannelItemQueryDirection.EARLIEST);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    public Optional<Object> getLatestItem(String channelName) {
        String path = pathPartsBuilder.getChannelItemPathPartExtractor(getChannelUrl(channelName)).getPath();
        Object response = channelItemResourceClient.getDirectionalItem(path, ChannelItemQueryDirection.LATEST);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    @SneakyThrows
    private Optional<TimeQueryResult> getItemByTimeFromLocation(String path, Location location) {
        ChannelItemPathParts pathParts = pathPartsBuilder.buildFromItemUrl(path);
        Call<TimeQueryResult> response = channelItemResourceClient.getItemsSecondsPath(
                pathParts.getChannelName(),
                pathParts.getYear(),
                pathParts.getMonth(),
                pathParts.getDay(),
                pathParts.getHour(),
                pathParts.getMinute(),
                pathParts.getSecond(),
                location);
        Optional<TimeQueryResult> op = Optional.ofNullable(response.execute().body());
        op.filter(o -> o.get_links() != null);
        return op;
    }

    private String getChannelUrl(String channelName) {
        return getHubBaseUrl() + "channel/" + channelName;
    }

    private HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
    }

}
