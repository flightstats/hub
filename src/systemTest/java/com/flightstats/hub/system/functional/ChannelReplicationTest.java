package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Channel;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.utility.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;

@Slf4j
class ChannelReplicationTest extends DependencyInjector {
    @Inject
    @Named("test.data")
    private String testData;
    private static final String REPL_SOURCE = "REPL_SOURCE";
    private static final String REPL_DEST = "REPL_DEST";
    private String replicationSourceChannelName;
    private String replicationDestChannelName;
    private String itemUri;
    @Inject
    private StringHelper stringHelper;
    @Inject
    private ChannelService channelService;
    @Inject
    @Named("hub.url") String hubBaseUrl;

    @BeforeEach
    void before() {
        replicationSourceChannelName = stringHelper.randomAlphaNumeric(10) + REPL_SOURCE;
        replicationDestChannelName = stringHelper.randomAlphaNumeric(10) + REPL_DEST;
        String replicationSource = hubBaseUrl + "/channel/" + replicationSourceChannelName;
        Channel destination = Channel.builder()
                .name(replicationDestChannelName)
                .replicationSource(replicationSource).build();
        channelService.create(replicationSourceChannelName);
        channelService.createCustom(destination);
        itemUri = channelService.addItem(replicationSourceChannelName, testData);
        channelService.addItem(replicationSourceChannelName, testData);
        channelService.addItem(replicationSourceChannelName, testData);
    }

    @Test
    void replication_itemInBothChannels_item() {
        Awaitility.await()
                .pollInterval(Duration.FIVE_SECONDS)
                .atMost(Duration.ONE_MINUTE)
                .until(() -> {
                    Object objectFromSource = channelService.getItem(itemUri);
                    String destItem = itemUri.replace(replicationSourceChannelName, replicationDestChannelName);
                    Object objectFromDestination = channelService.getItem(destItem);
                    log.error("%%%%%%%%%%%%%%%%%%ORIGINAL {}", itemUri);
                    log.error("%%%%%%%%%%%%%%%%%%DEST {}", destItem);
                    log.error("##################### {}", objectFromSource);
                    log.error("$$$$$$$$$$$$$$$$$$$$ {}", objectFromDestination);
                    return objectFromSource.equals(objectFromDestination);
                });
    }



}
