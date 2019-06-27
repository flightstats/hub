package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.system.extension.SingletonTestInjectionExtension;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
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
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private static final List<String> channelItems = new ArrayList<>();
    private final String channelName = randomAlphaNumeric(10);
    private String extraChannelItem;
    private String shouldBeDeleted;
    @Inject
    private ChannelItemRetriever channelItemRetriever;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator channelItemCreator;
    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void setup() {
        hubLifecycle.setup(CUSTOM_YAML_FOR_HELM);
        DateTime historicalDate = new DateTime().minus(standardHours(25));
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
//                .mutableTime(historicalDate)
                .build();
        ChannelConfig channelConfig2 = ChannelConfig.builder()
                .name(channelName + "REPL")
                .maxItems(5)
                .replicationSource(channelConfigService.getChannelUrl(channelName))
                .build();
        channelConfigService.create(channelConfig);
        channelConfigService.create(channelConfig2);
        List<DateTime> historicalDates = IntStream.range(0, 5).mapToObj(historicalDate::minusMinutes).collect(Collectors.toList());
        SortedSet<ChannelItemWithBody> c = channelItemCreator.addHistoricalItems(channelName, historicalDates);
        channelItems.addAll(c.stream().map(ChannelItemWithBody::getUrl).collect(Collectors.toList()));
        ChannelConfig channelConfig3 = channelConfig.toBuilder()
                .mutableTime(historicalDate.minusMinutes(10))
                .build();
        extraChannelItem = channelItemCreator.addHistoricalItem(channelName, historicalDate.minusMinutes(6), randomAlphaNumeric(5)).getUrl();
        channelConfigService.update(channelConfig3);
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
                        .allMatch(item -> channelItemRetriever.getItem(item).orElse("").equals(TEST_DATA)));
    }

    @Test
    @Order(0)
    void channelMaxItems_addsMaxItemsPlusOne_maxItemsStored() {
        shouldBeDeleted = channelItems.get(0);
        log.info("channel items: {}", channelItems);
        log.info("to delete? {}", shouldBeDeleted);
        List<String> nextItems = channelItems.subList(1, channelItems.size());
        nextItems.add(extraChannelItem);
        log.info("extraChannelItem: {}", extraChannelItem);
        verifyItemList(nextItems);
    }
//
//    @Test
//    @Order(1)
//    void channelMaxItems_getFirstItem_404() {
//        Awaitility.await()
//                .pollInterval(Duration.TEN_SECONDS)
//                .atMost(new Duration(5, TimeUnit.MINUTES))
//                .until(() -> {
//                    Optional<Object> f = channelItemRetriever.getItem(shouldBeDeleted);
//                    log.info("$$$$$$$$$$$$$$ {}", f);
//                    List<String> g = new ArrayList<>(channelItems);
//                    g.add(extraChannelItem);
//                    g.forEach(item -> log.info("@@@@@ {}", channelItemRetriever.getItem(item).orElse("")));
//                    return f.isPresent() && f.get().equals("");
//                });
//    }
//
//    @Test
//    @Order(2)
//    void channelMaxItems_bulkGet_containsMaxItemsWithoutFirstItem() {
//
//    }
}
