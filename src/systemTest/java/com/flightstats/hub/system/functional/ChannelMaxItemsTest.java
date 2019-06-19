package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
class ChannelMaxItemsTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private static final List<String> channelItems = new LinkedList<>();
    private final String channelName = randomAlphaNumeric(10);
    private String extraChannelItem;
    private String shouldBeDeleted;
    @Inject
    private ChannelService channelService;

    @BeforeAll
    void setup() {
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .maxItems(5)
                .build();
        channelService.create(channelConfig);
        channelItems.addAll(channelService.addItems(channelName, TEST_DATA, 5));
    }

    @AfterAll
    void cleanup() {
//        channelService.delete(channelName);
    }

    @SneakyThrows
    private void verifyItemList(List<String> items) {
        Awaitility.await()
                .atMost(Duration.ONE_MINUTE)
                .until(() -> items.parallelStream()
                        .allMatch(item -> channelService.getItem(item).orElse("").equals(TEST_DATA)));
    }

    @Test
    @Order(0)
    void channelMaxItems_addsMaxItemsPlusOne_maxItemsStored() {
        verifyItemList(channelItems);
        extraChannelItem = channelService.addItem(channelName, TEST_DATA);
        shouldBeDeleted = channelItems.get(0);
        List<String> nextItems = channelItems.subList(1, channelItems.size());
        nextItems.add(extraChannelItem);
        verifyItemList(nextItems);
    }

    @Test
    @Order(1)
    void channelMaxItems_getFirstItem_404() {
        Awaitility.await()
                .atMost(Duration.ONE_MINUTE)
                .until(() -> {
                    Optional<Object> f = channelService.getItem(shouldBeDeleted);
                    log.info("$$$$$$$$$$$$$$ {}", f);
                    return f.isPresent() && f.get().equals("");
                });
    }
//
//    @Test
//    @Order(2)
//    void channelMaxItems_bulkGet_containsMaxItemsWithoutFirstItem() {
//
//    }
}
