package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.kubernetes.PodRestart;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.system.config.HelmProperties;
import com.flightstats.hub.system.extension.TestSingletonClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import com.flightstats.hub.system.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class ClusterRollingRestartTest extends TestSingletonClassWrapper {
    @Inject
    HubInternalService hubInternalService;
    @Inject
    PodRestart podRestart;
    @Inject
    HelmProperties helmProperties;
    @Inject
    ChannelConfigService channelConfigService;
    @Inject
    ChannelItemCreator itemCreator;
    @Inject
    ChannelItemRetriever itemRetriever;
    @Inject
    S3Service s3Service;
    private ExecutorService insertExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService restartExecutor = Executors.newSingleThreadExecutor();
    private static final List<String> hubNodes = Arrays.asList("hub-0", "hub-1", "hub-2");
    private static final Map<String, List<String>> channels = new HashMap<>();
    private final CopyOnWriteArrayList<String> items = new CopyOnWriteArrayList<>();
    private static final String TEST_DATA = "TEST_DATA";

    private void addItemToChannel(String channelName) {
        Awaitility.await().pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS)
                .atMost(Duration.FIVE_SECONDS)
                .until(() -> {
                    try {
                        String itemUri = itemCreator.addItem(channelName, TEST_DATA).getItemUrl();
                        boolean success = itemUri != null;
                        if (success) {
                            items.add(itemUri);
                        }
                        return success;
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        return false;
                    }
                });
    }

    @AfterEach
    void cleanup2() {
        items.clear();
//        channelConfigService.delete(channelName);
    }

    boolean restartedNodeIsBack(String node) {
        boolean rejoined = hubInternalService.hasServerName(node);
        if (rejoined) {
            log.info("{} has rejoined the cluster", node);
        }
        return rejoined;
    }

    void restartNode(String node) {
        podRestart.execute(helmProperties.getReleaseName(),
                Collections.singletonList(node));
    }

    void restartNodes() {
        for (String node: hubNodes) {
            restartNode(node);
            Awaitility.await()
                    .pollInterval(Duration.TEN_SECONDS)
                    .atMost(Duration.FIVE_MINUTES)
                    .ignoreExceptions()
                    .until(() -> restartedNodeIsBack(node));
        }
    }


//    Boolean verifyItems(String item, ChannelType type) {
//        boolean inSpoke = itemRetriever.getItem(item).orElse("").equals(TEST_DATA);
//        if (type.equals(ChannelType.BOTH)) {
//            return s3Service.confirmItemInS3(ChannelType.SINGLE, item, channelName) &&
//                    s3Service.confirmItemInS3(ChannelType.BATCH, item, channelName)
//                    && inSpoke;
//        } else {
//            return s3Service.confirmItemInS3(type, item, channelName) && inSpoke;
//        }
//    }

    void tryToCreateAChannel(ChannelConfig channel) {
        int tries = 5;
        while (tries > 0) {
            try {
                channelConfigService.create(channel);
                channels.add(channel.getName());
                break;
            } catch (Exception e) {
                log.info("failed to create channel with error {}," +
                        "will retry {} more times", e.getMessage(), tries);
            } finally {
                tries = tries - 1;
            }
        }
    }

    void createChannels(ChannelType type) {
        IntStream.rangeClosed(1, 10)
                .forEach((i) -> {
                    String channelName = randomAlphaNumeric(10);
                    ChannelConfig channel = ChannelConfig.builder()
                            .name(channelName)
                            .storage(type.toString()).build();
                    tryToCreateAChannel(channel);
                });

    }

    void addItemstoChannels() {
        channels.forEach((channelName, list) -> {
            IntStream.rangeClosed(1, 200)
                    .forEach((i) -> addItemToChannel(channelName));
        });
    }

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void channelStorage_itemInSpokeAndS3_item(ChannelType type) {
        createChannels(type);
        restartExecutor.submit(this::restartNodes);


//        for (String item: items) {
//            Awaitility.await()
//                    .pollInterval(Duration.TEN_SECONDS)
//                    .atMost(Duration.TWO_MINUTES)
//                    .until(() -> verifyItems(item, type));
//        }
    }
}
