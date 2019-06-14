package com.flightstats.hub.system.functional;

import com.flightstats.hub.system.extension.DependencyInjectionExtension;
import com.flightstats.hub.system.extension.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.system.extension.DependencyInjectionResolver;
import com.flightstats.hub.system.extension.GuiceProviderExtension;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.fail;
import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
@ExtendWith(GuiceProviderExtension.class)
@ExtendWith(DependencyInjectionResolver.class)
@ExtendWith(HubLifecycleSuiteExtension.class)
@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageTest {
    private static final String TEST_DATA = "TEST_DATA";
    private String channelName;
    private String itemUri;
    @Inject
    private ChannelService channelService;
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
                    channelService.create(channel);
                    itemUri = channelService.addItem(channelName, TEST_DATA);
                    return itemUri != null;
                });
    }

    @AfterEach
    void cleanup() {
        channelService.delete(channelName);
    }

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void bothChannelStorage_itemInSpoke_item(ChannelType type) {
        fail();
        createAndAddItemsToChannel(type);
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> channelService.getItem(itemUri).orElse("").equals(TEST_DATA));
    }

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void bothChannelStorage_itemInS3_item(ChannelType type) {
        fail();
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

