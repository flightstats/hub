package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import org.joda.time.DateTime;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
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
    private final ChannelItemPathPartsBuilder pathPartsBuilder;

    @Inject
    public ChannelItemCreator(HubClientFactory hubClientFactory) {
        HttpUrl hubBaseUrl = hubClientFactory.getHubBaseUrl();
        this.pathPartsBuilder = new ChannelItemPathPartsBuilder(hubBaseUrl.toString());

        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
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
    public ChannelItemWithBody addHistoricalItem(String channelName, DateTime mutableInsertionTime, String itemIdentifier) {
        Call<ChannelItem> call = channelItemResourceClient.addHistorical(
                channelName,
                TimeUtil.millis(mutableInsertionTime) + itemIdentifier,
                itemIdentifier);

        Response<ChannelItem> response = call.execute();
        log.info("channel item creation response {} ", response);
        if (response.errorBody() != null) {
            try {
                log.info(new String(response.errorBody().bytes()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                //
            }
        }
        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return ChannelItemWithBody.builder()
                .url(response.body().get_links().getSelf().getHref())
                .body(itemIdentifier)
                .build();
    }

    public SortedSet<ChannelItemWithBody> addHistoricalItems(String channelName, List<DateTime> insertionDates) {
        Comparator<ChannelItemWithBody> comparator = Comparator.comparing(item ->
                pathPartsBuilder.buildFromItemUrl(item.getUrl()).getDateTime()
        );
        return insertionDates.stream()
                .map(time -> addHistoricalItem(channelName, time, randomAlphanumeric(5)))
                .collect(Collectors.toCollection(() -> new TreeSet<>(comparator)));
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
}
