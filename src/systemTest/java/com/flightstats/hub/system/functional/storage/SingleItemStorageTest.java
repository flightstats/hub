package com.flightstats.hub.system.functional.storage;

import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelStorage;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.S3Service;
import com.flightstats.hub.utility.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
class SingleItemStorageTest extends DependencyInjector {

    @Inject
    @Named("test.data")
    private String testData;
    private String channelName;
    private String itemUri;
    @Inject
    private StringHelper stringHelper;
    @Inject
    private ChannelService channelService;
    @Inject
    private S3Service s3Service;

    @BeforeEach
    void before() {
        channelName = stringHelper.randomAlphaNumeric(10);
        Channel channel = Channel.builder()
                .name(channelName)
                .storage(ChannelStorage.SINGLE.toString()).build();
        channelService.createCustom(channel);
        itemUri = channelService.addItem(channelName, testData);
    }

    @AfterEach
    void cleanup() {
//        channelService.delete(channelName);
    }

    @Test
    void singleChannelStorage_itemInSpoke_item() {
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> channelService.getItem(itemUri).equals(testData));
    }

    @Test
    void singleChannelStorage_itemInCache_item() {
        Awaitility.await()
                .pollInterval(Duration.TWO_SECONDS)
                .atMost(new Duration(20, TimeUnit.SECONDS))
                .until(() -> channelService.confirmItemInCache(itemUri, Location.CACHE_WRITE));
        // single items don't get added to the read cache
        assertFalse(channelService.confirmItemInCache(itemUri, Location.CACHE_READ));
    }

    private void confirmInCacheOrS3() {
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(Duration.TWO_MINUTES)
                .until(() -> s3Service.confirmItemsInS3(ChannelStorage.SINGLE, itemUri, "unusedForSingle"));

        // expect item to be deleted from spoke write cache
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(new Duration(260, TimeUnit.SECONDS))
                .until(() -> !channelService.confirmItemInCache(itemUri, Location.CACHE_WRITE));
    }

    @Test
    void singleChannelStorage_itemInS3_item() {
        confirmInCacheOrS3();
    }
}
