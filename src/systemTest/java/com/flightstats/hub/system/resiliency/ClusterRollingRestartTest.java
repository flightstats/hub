package com.flightstats.hub.system.resiliency;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.kubernetes.ExecPodCommand;
import com.flightstats.hub.kubernetes.PodRestart;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelItemPathParts;
import com.flightstats.hub.model.ChannelType;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.system.config.HelmProperties;
import com.flightstats.hub.system.config.ServiceProperties;
import com.flightstats.hub.system.extension.TestSingletonClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.HubInternalService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static java.lang.Runtime.getRuntime;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class ClusterRollingRestartTest extends TestSingletonClassWrapper {
    public static final String ZK_NODE_COUNT = "5";
    public static final String HUB_NODE_COUNT = "5";
    public static final String S3_VERIFIER_OFFSET = "1";
    public static final String SPOKE_WRITE_MINUTES = "3";
    private static final int ITEMS_PER_CHANNEL = 400;
    private static final int NUMBER_OF_CHANNELS = 250;
    private static final int SETUP_TIMEOUT_MINUTES = 30;
    private static final int ITEM_INSERT_PARALLELISM = getRuntime().availableProcessors();
    private static final CountDownLatch latch = new CountDownLatch(2);
    @Inject
    HubClientFactory hubClientFactory;
    @Inject
    ServiceProperties serviceProperties;
    @Inject
    PodRestart podRestart;
    @Inject
    HubInternalService hubInternalService;
    @Inject
    HelmProperties helmProperties;
    @Inject
    ChannelConfigService channelConfigService;
    @Inject
    ChannelItemCreator itemCreator;
    @Inject
    ChannelItemRetriever itemRetriever;
    private ExecutorService insertExecutor = Executors.newFixedThreadPool(ITEM_INSERT_PARALLELISM);
    private ExecutorService restartExecutor = Executors.newSingleThreadExecutor();
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> channels = new ConcurrentHashMap<>();
    private static final String TEST_DATA = "TEST_DATA";
    private static final String[] command = {};
    private static final ExecPodCommand execPodCommand = new ExecPodCommand("", "", command);

    private void addItemToChannel(String channelName) {
        try {
            String itemUri = itemCreator.addItemIgnoreFail(channelName, TEST_DATA).getItemUrl();
            boolean success = itemUri != null;
            if (success) {
                channels.get(channelName).add(itemUri);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @AfterEach
    void cleanup2() {
        channels.clear();
    }

    void waitForNodeToReturnHealthy(String node) {
        Awaitility.await()
                .atMost(new Duration(4, TimeUnit.MINUTES))
                .pollInterval(Duration.ONE_SECOND)
                .until(() -> hubInternalService.hasServerName(node));
    }

    void staggeredRestartNodes(List<String> nodes) {
        for (String node: nodes) {
            log.info("restart process starting for {}", nodes);
            podRestart.execute(helmProperties.getReleaseName(),
                    Collections.singletonList(node));
            waitForNodeToReturnHealthy(node);
            log.info("node {} has returned healthy", node);
        }
    }

    void recommission(String ip) {
        for (char i: "12345".toCharArray()) {
            try {
                boolean success = hubInternalService.recommissionNode(ip);
                if (!success) {
                    log.info("recommission did not return 202 for node {}", ip);
                } else {
                    break;
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
    }

    void decommissionNodes(List<String> nodes) {
        nodes.forEach(node -> {
                    try {
                        String[] decommission = {"curl",
                                "-X POST",
                                "http://localhost:8080/internal/cluster/decommission"};
                        execPodCommand.withNamespace(helmProperties.getReleaseName())
                                .withPod(node)
                                .withCommand(decommission)
                                .run();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                });
    }

    void waitForDoNotStartState(int count) {
        Awaitility.await()
                .pollInterval(Duration.TEN_SECONDS)
                .atMost(Duration.TEN_MINUTES)
                .until(() -> {
                    try {
                        List<String> servers = hubInternalService.getDoNotStartStateNodeIPs();
                        if (servers.size() == count) {
                            servers.forEach(this::recommission);
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        return false;
                    }
                });
    }



    void waitForRestartOnAllNodes() {
        List<String> threeNodes = Arrays.asList("hub-0", "hub-1", "hub-2");
        decommissionNodes(threeNodes);
        waitForDoNotStartState(3);
        staggeredRestartNodes(threeNodes);
        List<String> twoNodes = Arrays.asList("hub-3", "hub-4");
        decommissionNodes(twoNodes);
        waitForDoNotStartState(2);
        staggeredRestartNodes(twoNodes);
        latch.countDown();
    }


    Boolean verifyItems(String item) {
        log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        itemRetriever.getBulkMinuteFromLocation(item, Location.LONG_TERM_SINGLE)
                .ifPresent(i -> log.info("long term single present {}", i));
        itemRetriever.getBulkMinuteFromLocation(item, Location.LONG_TERM_BATCH)
                .ifPresent(i -> log.info("long term batch present {}", i));
        List<Optional<Object>> items = Arrays.asList(
                itemRetriever.getBulkMinuteFromLocation(item, Location.LONG_TERM),
                itemRetriever.getBulkMinuteFromLocation(item, Location.LONG_TERM_BATCH));
        return items.stream().allMatch(a ->
                a.isPresent() &&
                items
                .stream()
                .allMatch(b ->
                        b.isPresent() &&
                        a.get().equals(b.get())));
    }

    void createChannels() {
        IntStream.rangeClosed(1, 3).forEach((n) -> {
            IntStream.rangeClosed(1, NUMBER_OF_CHANNELS / 3)
                    .forEach((i) -> {
                        String channelName = randomAlphaNumeric(10);
                        ChannelConfig channel = ChannelConfig.builder()
                                .name(channelName)
                                .storage(ChannelType.BOTH.toString())
                                .build();
                        channelConfigService.create(channel);
                        channels.put(channelName, new CopyOnWriteArrayList<>());
                    });
        });

    }

    void addItemsToChannels() {
        channels.forEach(ITEM_INSERT_PARALLELISM,
                (channelName, list) -> IntStream.rangeClosed(1, ITEMS_PER_CHANNEL)
                    .forEach((i) -> addItemToChannel(channelName)));
        latch.countDown();
    }

    String getMinutePath(String path) {
        ChannelItemPathParts pathParts = ChannelItemPathParts
                .builder()
                .itemUrl(path)
                .baseUrl(hubClientFactory.getHubBaseUrl())
                .build();
        return String.format("%s/%s/%s/%s/%s/%s/%s",
                serviceProperties.getHubUrl(),
                pathParts.getChannelName(),
                pathParts.getYear(),
                pathParts.getMonth(),
                pathParts.getDay(),
                pathParts.getHour(),
                pathParts.getMinute());
    }

    List<String> getPathsForMinutePaths() {
        List<String> values = new ArrayList<>();
        channels.forEach((String key, CopyOnWriteArrayList<String> val) -> values.addAll(val));
        List<String> minutePaths = values.stream().map(this::getMinutePath)
                .distinct()
                .collect(Collectors.toList());
        List<String> paths = values.stream().filter((String val) -> {
            String minutePath =  getMinutePath(val);
            if (minutePaths.contains(minutePath)) {
                minutePaths.remove(minutePath);
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        log.info("************* {}", paths);
        return paths;
    }

    @Test
    @SneakyThrows
    void channelStorage_itemInSpokeAndS3_item() {
        createChannels();
//        restartExecutor.submit(this::waitForRestartOnAllNodes);
        insertExecutor.submit(this::addItemsToChannels);
        latch.await(SETUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (latch.getCount() > 0) {
            throw new Exception("test timed out in set up phase");
        }
        getPathsForMinutePaths().forEach(item -> Awaitility.await()
                    .atMost(Duration.TWO_MINUTES)
                    .pollInterval(Duration.FIVE_SECONDS)
                    .until(() -> {
                        try {
                            log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                            return verifyItems(item);
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            return false;
                        }
                    }));
    }
}
