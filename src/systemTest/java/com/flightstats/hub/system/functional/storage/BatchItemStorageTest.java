package com.flightstats.hub.system.functional.storage;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelStorage;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.S3Service;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
class BatchItemStorageTest extends DependencyInjector {
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
    void setup() {
        channelName = randomAlphaNumeric(10);
        Channel channel = Channel.builder()
                .storage(ChannelStorage.BATCH.toString())
                .name(channelName).build();
        channelService.createCustom(channel);
        itemUri = channelService.addItem(channelName, testData);
    }

    @AfterEach
    void cleanup() {
        channelService.delete(channelName);
    }

    @Test
    void batchChannelStorage_itemInSpoke_item() {
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> channelService.getItem(itemUri).equals(testData));
    }

    @Test
    void batchChannelStorage_itemInS3_item() {
        Awaitility.await()
                .atMost(Duration.TWO_MINUTES)
                .pollInterval(Duration.TEN_SECONDS)
                .until(() -> s3Service.confirmItemsInS3(ChannelStorage.BATCH, itemUri, channelName));
    }
}
