package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.clients.hub.channel.ChannelResourceClient;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.Links;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQueryResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import com.flightstats.hub.model.ChannelConfig;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ChannelService {

    private static final String CHANNEL_OWNER = "system-tests";
    private ChannelItemResourceClient channelItemResourceClient;
    private ChannelResourceClient channelResourceClient;
    private HttpUrl hubBaseUrl;

    @Inject
    public ChannelService(HubClientFactory hubClientFactory) {
        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
        this.channelResourceClient = hubClientFactory.getHubClient(ChannelResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    public String getChannelUrl(String channelName) {
        return getHubBaseUrl() + "channel/" + channelName;
    }

    private HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
    }

    @SneakyThrows
    public void createWithDefaults(String channelName) {
        log.info("Create channel name {} ", channelName);
        create(ChannelConfig.builder().name(channelName).owner(CHANNEL_OWNER).build());
    }

    @SneakyThrows
    public void create(ChannelConfig channel) {
        Call<Object> call = channelResourceClient.create(channel.toBuilder().owner(CHANNEL_OWNER).build());
        Response<Object> response = call.execute();
        log.info("channel creation response {}, channelName, {}", response, channel.getName());
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    public List<String> addItems(String channelName, Object data, int count) {
        List<String> channelItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            channelItems.add(addItem(channelName, data));
        }
        return channelItems;
    }

    @SneakyThrows
    public String addItem(String channelName, Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();
        log.info("channel item creation response {} ", response);
        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return response.body().get_links().getSelf().getHref();
    }

    @SneakyThrows
    public byte[] addItemError(String channelName) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, "anything");
        Optional<ResponseBody> optionalError = Optional.ofNullable(call.execute().errorBody());
        if (optionalError.isPresent()) {
            return optionalError.get().bytes();
        }
        return new byte[] {};
    }

    private Object getItemResponse(String path) {
        FormattedStringHelper helper = new FormattedStringHelper().withExtractedUrlPaths(path, hubBaseUrl.toString());
        return channelItemResourceClient.get(
                helper.getChannelName(),
                helper.getYear(),
                helper.getMonth(),
                helper.getDay(),
                helper.getHour(),
                helper.getMinute(),
                helper.getSecond(),
                helper.getMillis(),
                helper.getHashKey());
    }

    @SneakyThrows
    public byte[] getItemError(String path) {
        Object response = getItemResponse(path);
        Optional<ResponseBody> optionalError = Optional.ofNullable(((Call) response).execute().errorBody());
        if (optionalError.isPresent()) {
            return optionalError.get().bytes();
        }
        return new byte[] {};
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
        Object response = getItemResponse(path);
        return Optional.ofNullable(((Call) response).execute().body());
    }

    public boolean confirmItemInCache(String itemUri, Location location) {
        TimeQueryResult result = getItemByTimeFromLocation(itemUri, location)
                .orElse(TimeQueryResult.builder()._links(Links.builder().uris(new String[]{}).build()).build());
        List<String> uris = Arrays.asList(result.get_links().getUris());
        return uris.stream().anyMatch(str -> str.equals(itemUri));
    }

    @SneakyThrows
    public void delete(String channelName) {
        this.channelResourceClient.delete(channelName).execute();
    }
}