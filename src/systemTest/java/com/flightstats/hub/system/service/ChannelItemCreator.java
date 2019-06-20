package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItem;
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
public class ChannelItemCreator {
    private final ChannelItemResourceClient channelItemResourceClient;
    private final HttpUrl hubBaseUrl;

    @Inject
    public ChannelItemCreator(HubClientFactory hubClientFactory) {
        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    public String getChannelUrl(String channelName) {
        return getHubBaseUrl() + "channel/" + channelName;
    }

    private HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
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
        return new byte[]{};
    }
}
