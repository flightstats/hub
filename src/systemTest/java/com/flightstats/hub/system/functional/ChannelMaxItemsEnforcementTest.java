package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import com.flightstats.hub.system.service.S3Service;
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
    @Inject
    private S3Service s3Service;

    @AfterEach
    void cleanup() {
        items.clear();
        channelConfigService.delete(channelName);
    }

    @SneakyThrows
    private void addItemsOverTime() {
        /*
            batch items are compared with granularity of one minute
            so if the excess items are of the same minute they will not be deleted
        */
        items.add(channelItemCreator.addItem(channelName, TEST_DATA));
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        items.addAll(channelItemCreator.addItems(channelName, TEST_DATA, 5));
    }

    private void waitUntilAllItemsAreInS3(ChannelType type) {
        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(new Duration(3, TimeUnit.MINUTES))
                .until(() -> s3Service.confirmItemsInS3(type, items, channelName) &&
                        items.stream().allMatch(path -> channelItemRetriever.getItem(path).isPresent()));
    }

    private String setupTest(ChannelType type) {
        channelName = randomAlphaNumeric(10);
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(channelName)
                .storage(type.name())
                .maxItems(5)
                .build();
        channelConfigService.create(channelConfig);

        addItemsOverTime();
        String path = items.get(0);
        waitUntilAllItemsAreInS3(type);
        return path;
    }

    private void confirmDelete(ChannelType type, String path) {
        Awaitility.await().pollInterval(Duration.FIVE_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> s3Service.confirmItemNotInS3(type, path, channelName));
    }


    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void maxItemsTest_oldestItemDeletedFromS3_itemNotInS3(ChannelType type) {
        // GIVEN
        String path = setupTest(type);

        // WHEN
        hubInternalService.enforceMaxItems(channelName);


        // THEN
        confirmDelete(type, path);
    }
}



