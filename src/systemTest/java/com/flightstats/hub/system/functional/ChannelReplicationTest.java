package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.ChannelConfigService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class ChannelReplicationTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private static final String REPL_SOURCE = "REPL_TEST_SOURCE";
    private static final String REPL_DEST = "REPL_TEST_DEST";
    private String replicationSourceChannelName;
    private String replicationDestChannelName;
    private String itemUri1;
    private String itemUri2;
    private String itemUri3;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator itemCreator;
    @Inject
    private ChannelItemRetriever itemRetriever;

    @BeforeEach
    @SneakyThrows
    void before() {
        replicationSourceChannelName = randomAlphaNumeric(10) + REPL_SOURCE;
        replicationDestChannelName = randomAlphaNumeric(10) + REPL_DEST;
        String replicationSource = channelConfigService.getChannelUrl(replicationSourceChannelName);
        ChannelConfig destination = ChannelConfig.builder()
                .name(replicationDestChannelName)
                .replicationSource(replicationSource).build();
        channelConfigService.createWithDefaults(replicationSourceChannelName);
        channelConfigService.create(destination);
        // give the repl channel time to create it's webhook
        Thread.sleep(3500);
    }

    @AfterEach
    void cleanup() {
        channelConfigService.delete(replicationSourceChannelName);
        channelConfigService.delete(replicationDestChannelName);
    }

    @Test
    void replication_itemInBothChannels_item() {
        itemUri1 = itemCreator.addItem(replicationSourceChannelName, TEST_DATA);
        Object objectFromSource = itemRetriever.getItem(itemUri1);
        Awaitility.await()
                .pollInterval(Duration.ONE_SECOND)
                .atMost(new Duration(90, TimeUnit.SECONDS))
                .until(() -> {
                    String destUri = itemUri1.replace(replicationSourceChannelName, replicationDestChannelName);
                    Object result = itemRetriever.getItem(destUri);
                    return objectFromSource.equals(result);
                });
    }


    @Test
    void replication_itemsInBothChannels_item() {
        itemUri1 = itemCreator.addItem(replicationSourceChannelName, TEST_DATA);
        itemUri2 = itemCreator.addItem(replicationSourceChannelName, TEST_DATA);
        itemUri3 = itemCreator.addItem(replicationSourceChannelName, TEST_DATA);
        Object objectFromSource = itemRetriever.getItem(itemUri1);
        Awaitility.await()
                .pollInterval(Duration.FIVE_SECONDS)
                .atMost(new Duration(90, TimeUnit.SECONDS))
                .until(() -> Stream.of(itemUri1, itemUri2, itemUri3)
                            .map(uri -> uri.replace(replicationSourceChannelName, replicationDestChannelName))
                            .map(itemRetriever::getItem)
                            .allMatch(objectFromSource::equals));
    }

    @Test
    void replication_throwsOnAddItemToDestChannel_exception() {
        byte [] error = itemCreator.addItemError(replicationDestChannelName);
        assertEquals(new String(error), replicationDestChannelName + " cannot modified while replicating");
    }



}
