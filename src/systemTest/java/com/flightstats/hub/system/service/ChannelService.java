package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.clients.hub.channel.ChannelResourceClient;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.DatePathIndex;
import com.flightstats.hub.model.Links;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private HttpUrl getHubBaseUrl() {
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

    private List<String> getPathParts(String path) {
        String itemPath = path.replace(hubBaseUrl.toString() + "channel/", "");
        return Arrays.asList(itemPath.split("/"));
    }

    private Map<DatePathIndex, Integer> getPathDateParts(List<String> pathParts) {
        Map<DatePathIndex, Integer> dateValues = new HashMap<>();
        List<Integer> dateParts = pathParts.subList(1, pathParts.size() -1).stream()
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        dateValues.put(DatePathIndex.YEAR, dateParts.get(DatePathIndex.YEAR.getIndex()));
        dateValues.put(DatePathIndex.MONTH, dateParts.get(DatePathIndex.MONTH.getIndex()));
        dateValues.put(DatePathIndex.DAY, dateParts.get(DatePathIndex.DAY.getIndex()));
        dateValues.put(DatePathIndex.HOUR, dateParts.get(DatePathIndex.HOUR.getIndex()));
        dateValues.put(DatePathIndex.MINUTE, dateParts.get(DatePathIndex.MINUTE.getIndex()));
        dateValues.put(DatePathIndex.SECONDS, dateParts.get(DatePathIndex.SECONDS.getIndex()));
        dateValues.put(DatePathIndex.MILLIS, dateParts.get(DatePathIndex.MILLIS.getIndex()));
        return dateValues;
    }

    private Map<String, String> getPathKeys(List<String> pathParts) {
        Map<String, String> keys = new HashMap<>();
        keys.put("channelName", pathParts.get(0));
        keys.put("key", pathParts.get(pathParts.size() - 1));
        return keys;
    }

    @SneakyThrows
    private Optional<TimeQuery> getItemByTimeFromLocation(String path, Location location) {
        List<String> pathParts = getPathParts(path);
        Map<String, String> keys = getPathKeys(pathParts);
        Map<DatePathIndex, Integer> dateParts = getPathDateParts(pathParts);
        Call<TimeQuery> response = channelItemResourceClient.getItemsSecondsPath(keys.get("channelName"),
                dateParts.get(DatePathIndex.YEAR),
                dateParts.get(DatePathIndex.MONTH),
                dateParts.get(DatePathIndex.DAY),
                dateParts.get(DatePathIndex.HOUR),
                dateParts.get(DatePathIndex.MINUTE),
                dateParts.get(DatePathIndex.SECONDS),
                location);
        return Optional.ofNullable(response.execute().body());
    }

    @SneakyThrows
    public Object getItem(String path) {
        List<String> pathParts = getPathParts(path);
        Map<String, String> keys = getPathKeys(pathParts);
        Map<DatePathIndex, Integer> dateParts = getPathDateParts(pathParts);
        Object response = channelItemResourceClient.get(
                keys.get("channelName"),
                dateParts.get(DatePathIndex.YEAR),
                dateParts.get(DatePathIndex.MONTH),
                dateParts.get(DatePathIndex.DAY),
                dateParts.get(DatePathIndex.HOUR),
                dateParts.get(DatePathIndex.MINUTE),
                dateParts.get(DatePathIndex.SECONDS),
                dateParts.get(DatePathIndex.MILLIS),
                keys.get("key"));
        return ((Call) response).execute().body();
    }

    public boolean confirmItemInCache(String itemUri) {
        TimeQuery result = getItemByTimeFromLocation(itemUri, Location.CACHE)
                .orElse(TimeQuery.builder()._links(Links.builder().uris(new String[] {}).build()).build());
        List<String> uris = Arrays.asList(result.get_links().getUris());
        return uris.stream().anyMatch(str -> str.equals(itemUri));
    }

    @SneakyThrows
    public void delete(String channelName) {
        this.channelResourceClient.delete(channelName).execute();
    }
}