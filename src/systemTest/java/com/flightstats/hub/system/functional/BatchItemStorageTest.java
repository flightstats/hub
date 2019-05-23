package com.flightstats.hub.system.functional;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelStorage;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.S3Service;
import com.flightstats.hub.utility.StringHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

@Slf4j
class BatchItemStorageTest extends DependencyInjector {
    private static final String TEST_DATA = "TEST_DATA";
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
                .storage(ChannelStorage.BATCH.toString())
                .name(channelName).build();
        channelService.createCustom(channel);
        itemUri = channelService.addItem(channelName, TEST_DATA);
    }

    @AfterEach
    void cleanup() {
        channelService.delete(channelName);
    }

    @Test
    void batchChannelStorage_itemInSpoke_item() {
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> channelService.getItem(itemUri).equals(TEST_DATA));
    }

    @Test
    void batchChannelStorage_itemInS3_item() {
        String path = s3Service.formatS3BatchItemPath(itemUri, channelName);
        Awaitility.await()
                .atMost(Duration.TWO_MINUTES)
                .pollInterval(Duration.TEN_SECONDS)
                .until(() -> confirmItemsInS3(path));
    }

    @SneakyThrows
    private boolean confirmItemsInS3(String path) {
        try {
            byte[] result = s3Service.getS3BatchedItems(path);
            String actual = new String(result, StandardCharsets.UTF_8)
                    .replaceAll("\"", "")
                    .trim();
            if (!actual.equals(TEST_DATA)) {
                throw new Exception("actual does not match expected");
            }
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.error("error getting item {}", e.getMessage());
                throw e;
            }
            return false;
        }
    }
}
