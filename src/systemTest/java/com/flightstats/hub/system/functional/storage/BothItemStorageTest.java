package com.flightstats.hub.system.functional.storage;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelStorage;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.S3Service;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static com.flightstats.hub.system.config.PropertiesName.TEST_DATA;

class BothItemStorageTest extends DependencyInjector {
    @Inject
    @Named(TEST_DATA)
    private String testData;
    private String channelName;
    private String itemUri;
    @Inject
    private ChannelService channelService;
    @Inject
    private S3Service s3Service;
    @Inject
    private HubLifecycle hubLifecycle;

    @BeforeAll
    void helmSetup() { hubLifecycle.setup();}

    @AfterAll
    void helmCleanup() { hubLifecycle.cleanup(); }

    @BeforeEach
    void before() {
        channelName = randomAlphaNumeric(10);
        Channel channel = Channel.builder()
                .name(channelName)
                .storage(ChannelStorage.BOTH.toString()).build();
        channelService.createCustom(channel);
        itemUri = channelService.addItem(channelName, testData);
    }

    @AfterEach
    void cleanup() {
        channelService.delete(channelName);
    }

    @Test
    void bothChannelStorage_itemInSpoke_item() {
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> channelService.getItem(itemUri).equals(testData));
    }

    @Test
    void bothChannelStorage_itemInS3_item() {
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> s3Service.confirmItemsInS3(ChannelStorage.SINGLE, itemUri, "unusedForSingle") &&
                        s3Service.confirmItemsInS3(ChannelStorage.BATCH, itemUri, channelName));
    }
}
