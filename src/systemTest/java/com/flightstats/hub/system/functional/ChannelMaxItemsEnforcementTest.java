package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Execution(ExecutionMode.SAME_THREAD)
class ChannelMaxItemsEnforcementTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private static final int MAX_ITEMS = 5;
    private final List<String> items = new ArrayList<>();
    private String channelName;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator channelItemCreator;
    @Inject
    private ChannelItemRetriever channelItemRetriever;
    @Inject
    private HubInternalService hubInternalService;

    @AfterEach
    void cleanup() {
        items.clear();
        channelConfigService.delete(channelName);
    }

    private boolean confirmInLocation(String item, Location location) {
        return channelItemRetriever.getItemFromLocation(item, location).filter(i -> i.equals(TEST_DATA)).isPresent();
    }

    private void waitUntilAllItemsAreInS3(ChannelType type) {
        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(new Duration(3, TimeUnit.MINUTES))
                .until(() -> {
                    if (type.equals(ChannelType.BOTH)) {
                        return items.stream().allMatch(item -> confirmInLocation(item, Location.LONG_TERM_SINGLE)) &&
                                items.stream().allMatch(item -> confirmInLocation(item, Location.LONG_TERM_BATCH));
                    } else {
                        Location location = type.equals(ChannelType.BATCH) ? Location.LONG_TERM_BATCH : Location.LONG_TERM_SINGLE;
                        return items.stream().allMatch(item -> confirmInLocation(item, location));
                    }
                });
    }

    private void createChannelWithMaxItems(ChannelType type) {
        channelName = randomAlphaNumeric(10);
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .storage(type.name())
                .maxItems(MAX_ITEMS)
                .build();
        channelConfigService.create(channelConfig);
    }

    private void addMaxItems() {
        items.addAll(channelItemCreator.addItems(channelName, TEST_DATA, MAX_ITEMS));
    }

    @SneakyThrows
    private void waitAMinute(ChannelType type) {
        /*
            batch items are compared with granularity of one minute
            so if the excess items are of the same minute they will not be deleted
            TODO: a different solution and a fix for the root cause
        */
        if (!type.equals(ChannelType.SINGLE)) {
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        }
    }

    private String addExtraItem() {
        String item = channelItemCreator.addItem(channelName, TEST_DATA);
        items.add(item);
        return item;
    }

    private void confirmDelete(ChannelType type, String path) {
        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> {
                    if (type.equals(ChannelType.BOTH)) {
                        return !confirmInLocation(path, Location.LONG_TERM_SINGLE) &&
                                !confirmInLocation(path, Location.LONG_TERM_BATCH);
                    } else {
                        Location location = type.equals(ChannelType.BATCH) ? Location.LONG_TERM_BATCH : Location.LONG_TERM_SINGLE;
                        return !confirmInLocation(path, location);
                    }
                });
    }


    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void maxItemsTest_oldestItemDeletedFromS3_itemNotInS3(ChannelType type) {
        // GIVEN
        createChannelWithMaxItems(type);
        addMaxItems();
        waitAMinute(type);
        String path = addExtraItem();
        waitUntilAllItemsAreInS3(type);

        // WHEN
        hubInternalService.enforceMaxItems(channelName);


        // THEN
        confirmDelete(type, path);
    }
}



