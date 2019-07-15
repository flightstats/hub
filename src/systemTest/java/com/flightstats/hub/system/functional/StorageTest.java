package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
class StorageTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private String channelName;
    private String itemUri;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator itemCreator;
    @Inject
    private ChannelItemRetriever itemRetriever;
    @Inject
    private S3Service s3Service;

    @BeforeEach
    void setup() {
        channelName = randomAlphaNumeric(10);
    }

    private void createAndAddItemsToChannel(ChannelType type) {
        Awaitility.await().pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS)
                .atMost(Duration.FIVE_SECONDS)
                .until(() -> {
                    channelName = randomAlphaNumeric(10);
                    ChannelConfig channel = ChannelConfig.builder()
                            .name(channelName)
                            .storage(type.toString()).build();
                    channelConfigService.create(channel);
                    itemUri = itemCreator.addItem(channelName, TEST_DATA).getItemUrl();
                    return itemUri != null;
                });
    }

    @AfterEach
    void cleanup() {
        channelConfigService.delete(channelName);
    }

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void bothChannelStorage_itemInSpoke_item(ChannelType type) {
        createAndAddItemsToChannel(type);
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> itemRetriever.getItem(itemUri).orElse("").equals(TEST_DATA));
    }

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void bothChannelStorage_itemInS3_item(ChannelType type) {
        createAndAddItemsToChannel(type);
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> {
                    if (type.equals(ChannelType.BOTH)) {
                        return s3Service.confirmItemsInS3(ChannelType.SINGLE, itemUri, channelName) &&
                                s3Service.confirmItemsInS3(ChannelType.BATCH, itemUri, channelName);
                    } else {
                        return s3Service.confirmItemsInS3(type, itemUri, channelName);
                    }
                });
    }
}

