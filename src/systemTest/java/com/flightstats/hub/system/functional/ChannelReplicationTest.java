package com.flightstats.hub.system.functional;

import com.flightstats.hub.system.extension.DependencyInjectionExtension;
import com.flightstats.hub.system.extension.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.system.extension.DependencyInjectionResolver;
import com.flightstats.hub.system.extension.GuiceProviderExtension;
import com.flightstats.hub.system.service.ChannelService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@ExtendWith(GuiceProviderExtension.class)
@ExtendWith(DependencyInjectionResolver.class)
@ExtendWith(HubLifecycleSuiteExtension.class)
@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChannelReplicationTest {
    private static final String TEST_DATA = "TEST_DATA";
    private static final String REPL_SOURCE = "REPL_TEST_SOURCE";
    private static final String REPL_DEST = "REPL_TEST_DEST";
    private String replicationSourceChannelName;
    private String replicationDestChannelName;
    private String itemUri1;
    private String itemUri2;
    private String itemUri3;
    @Inject
    private ChannelService channelService;

    @BeforeEach
    @SneakyThrows
    void before() {
        replicationSourceChannelName = randomAlphaNumeric(10) + REPL_SOURCE;
        replicationDestChannelName = randomAlphaNumeric(10) + REPL_DEST;
        String replicationSource = channelService.getChannelUrl(replicationSourceChannelName);
        ChannelConfig destination = ChannelConfig.builder()
                .name(replicationDestChannelName)
                .replicationSource(replicationSource).build();
        channelService.createWithDefaults(replicationSourceChannelName);
        channelService.create(destination);
        // give the repl channel time to create it's webhook
        Thread.sleep(3500);
    }

    @AfterEach
    void cleanup() {
        channelService.delete(replicationSourceChannelName);
        channelService.delete(replicationDestChannelName);
    }

    @Test
    void replication_itemInBothChannels_item() {
        itemUri1 = channelService.addItem(replicationSourceChannelName, TEST_DATA);
        Object objectFromSource = channelService.getItem(itemUri1);
        Awaitility.await()
                .pollInterval(Duration.ONE_SECOND)
                .atMost(new Duration(90, TimeUnit.SECONDS))
                .until(() -> {
                    String destUri = itemUri1.replace(replicationSourceChannelName, replicationDestChannelName);
                    Object result = channelService.getItem(destUri);
                    return objectFromSource.equals(result);
                });
    }


    @Test
    void replication_itemsInBothChannels_item() {
        itemUri1 = channelService.addItem(replicationSourceChannelName, TEST_DATA);
        itemUri2 = channelService.addItem(replicationSourceChannelName, TEST_DATA);
        itemUri3 = channelService.addItem(replicationSourceChannelName, TEST_DATA);
        Object objectFromSource = channelService.getItem(itemUri1);
        Awaitility.await()
                .pollInterval(Duration.FIVE_SECONDS)
                .atMost(new Duration(90, TimeUnit.SECONDS))
                .until(() -> Stream.of(itemUri1, itemUri2, itemUri3)
                            .map(uri -> uri.replace(replicationSourceChannelName, replicationDestChannelName))
                            .map(channelService::getItem)
                            .allMatch(objectFromSource::equals));
    }

    @Test
    void replication_throwsOnAddItemToDestChannel_exception() {
        byte [] error = channelService.addItemError(replicationDestChannelName);
        assertEquals(new String(error), replicationDestChannelName + " cannot modified while replicating");
    }



}
