package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.system.extension.SingletonTestInjectionExtension;
import com.flightstats.hub.system.service.ChannelService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.joda.time.Duration.standardHours;

@Slf4j
@ExtendWith(SingletonTestInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChannelMaxItemsTest {
    private static final String CUSTOM_YAML_FOR_HELM = "hub: \n" +
            "  hub: \n" +
            "    image: flightstats/hub:2019.06.21.1.s3-lifecycle-rules-offset \n" +
            "    configMap: \n" +
            "      properties: \n" +
            "        s3.ttlEnforcer.offset.minutes: 2";
    private static final String TEST_DATA = "TEST_DATA";
    private static final List<String> channelItems = new LinkedList<>();
    private final String channelName = randomAlphaNumeric(10);
    private String extraChannelItem;
    private String shouldBeDeleted;
    @Inject
    private ChannelService channelService;
    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void setup() {
        hubLifecycle.setup(CUSTOM_YAML_FOR_HELM);
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .mutableTime(new DateTime().minus(standardHours(25)))
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
        log.info("channel items: {}", channelItems);
        log.info("to delete? {}", shouldBeDeleted);
        List<String> nextItems = channelItems.subList(1, channelItems.size());
        nextItems.add(extraChannelItem);
        log.info("extraChannelItem: {}", extraChannelItem);
        verifyItemList(nextItems);
    }

    @Test
    @Order(1)
    void channelMaxItems_getFirstItem_404() {
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(new Duration(5, TimeUnit.MINUTES))
                .until(() -> {
                    Optional<Object> f = channelService.getItem(shouldBeDeleted);
                    log.info("$$$$$$$$$$$$$$ {}", f);
                    List<String> g = new ArrayList<>(channelItems);
                    g.add(extraChannelItem);
                    g.forEach(item -> log.info("@@@@@ {}", channelService.getItem(item).orElse("")));
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
