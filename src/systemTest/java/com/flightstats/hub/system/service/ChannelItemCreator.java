package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ChannelItemPathParts;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
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
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();

        this.channelItemResourceClient = hubClientFactory.getHubClient(ChannelItemResourceClient.class);
    }

    public List<ChannelItemWithBody> addItems(String channelName, String data, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> addItem(channelName, data))
                .collect(toList());
    }

    public List<ChannelItemWithBody> addItems(String channelName, List<String> itemData) {
        return itemData.stream()
                .map(data -> addItem(channelName, data))
                .collect(toList());
    }

    @SneakyThrows
    public ChannelItemWithBody addItem(String channelName, Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();
        log.info("channel item creation response {} ", response);
        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return buildChannelItem(response, data);
    }

    @SneakyThrows
    public ChannelItemWithBody addHistoricalItem(String channelName, DateTime mutableInsertionTime, String itemIdentifier) {
        Call<ChannelItem> call = channelItemResourceClient.addHistorical(
                channelName,
                TimeUtil.millis(mutableInsertionTime) + itemIdentifier,
                itemIdentifier);

        Response<ChannelItem> response = call.execute();
        log.info("channel item creation response {} ", response);
        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return buildChannelItem(response, itemIdentifier);
    }

    public SortedSet<ChannelItemWithBody> addHistoricalItems(String channelName, List<DateTime> insertionDates) {
        Comparator<ChannelItemWithBody> comparator = Comparator.comparing(this::getDateTimeForItem);
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

    private DateTime getDateTimeForItem(ChannelItemWithBody channelItem) {
        return ChannelItemPathParts.builder()
                .itemUrl(channelItem.getPath())
                .baseUrl(hubBaseUrl)
                .build()
                .getDateTime();
    }

    private ChannelItemWithBody buildChannelItem(Response<ChannelItem> response, Object body) {
        return ChannelItemWithBody.builder()
                .baseUrl(hubBaseUrl)
                .itemUrl(response.body().get_links().getSelf().getHref())
                .body(body)
                .build();
    }

}
