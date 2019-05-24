package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.clients.hub.channel.ChannelResourceClient;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.DatePathIndex;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ChannelService {

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

    public HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
    }

    @SneakyThrows
    public void create(String channelName) {
        log.info("Create channel name {} ", channelName);
        createCustom(Channel.builder().name(channelName).build());
    }

    @SneakyThrows
    public void createCustom(Channel channel) {
        Call<Object> call = channelResourceClient.create(channel);
        Response<Object> response = call.execute();
        log.info("channel creation response {} ", response);
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    public List<String> addItems(String channelName, Object data, int count) {
        final List<String> channelItems = new ArrayList<>();
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
    public Object getItem(String path) {
        String itemPath = path.replace(hubBaseUrl.toString() + "channel/", "");
        List<String> pathParts = new LinkedList<>(Arrays.asList(itemPath.split("/")));
        String channelName = pathParts.remove(0);
        String itemKey = pathParts.remove(pathParts.size() - 1);
        List<Integer> dateParts = pathParts.stream().filter(StringUtils::isNotBlank).map(Integer::parseInt).collect(Collectors.toList());
        int year = dateParts.get(DatePathIndex.YEAR.getIndex());
        int month = dateParts.get(DatePathIndex.MONTH.getIndex());
        int day = dateParts.get(DatePathIndex.DAY.getIndex());
        int hour = dateParts.get(DatePathIndex.HOUR.getIndex());
        int minute = dateParts.get(DatePathIndex.MINUTE.getIndex());
        int seconds = dateParts.get(DatePathIndex.SECONDS.getIndex());
        int millis = dateParts.get(DatePathIndex.MILLIS.getIndex());
        Object response = channelItemResourceClient.get(channelName, year, month, day, hour, minute, seconds, millis, itemKey);
        return ((Call) response).execute().body();
    }

    @SneakyThrows
    public void delete(String channelName) {
        this.channelResourceClient.delete(channelName).execute();
    }
}