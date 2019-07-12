package com.flightstats.hub.system.functional;

import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.InternalZookeeperService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

class ChannelLatestUpdatedStateTest extends TestClassWrapper {
    private String channelName;

    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemRetriever channelItemRetriever;
    @Inject
    private ChannelItemCreator channelItemCreator;
    @Inject
    private InternalZookeeperService zookeeperService;
    @Inject
    private ModelBuilder modelBuilder;

    @BeforeEach
    private void setup() {
        channelName = randomAlphaNumeric(10);
        channelConfigService.createWithDefaults(channelName);
    }

    @AfterEach
    void cleanup() {
        channelConfigService.delete(channelName);
    }

    @Test
    void channelWithNoItem_hasStateValueOfEpochNone_and_hasNoChannelLatest() {
        Optional<String> latestItem = channelItemRetriever.getLatestUnstableItemUrl(channelName);
        assertFalse(latestItem.isPresent());

        String channelLatestUpdated = zookeeperService.getChannelLatestUpdated(channelName)
                .orElseThrow(AssertionError::new);
        assertEquals("1970/01/01/00/00/00/001/none", channelLatestUpdated);
    }

    @Test
    void channelWithItemsInSpoke_hasZkLatestItemsEqualToLastInsertedItemAfterQueryingLatestEndpoint() {
        List<String> data = channelItemCreator.addItems(channelName, "data", 3);
        String expectedLastItemUrl = data.stream().min(Comparator.reverseOrder())
                .orElseThrow(AssertionError::new);

        String latestItemUrl = channelItemRetriever.getLatestUnstableItemUrl(channelName)
                .orElseThrow(AssertionError::new);
        assertEquals(expectedLastItemUrl, latestItemUrl);

        String channelLatestUpdated = zookeeperService.getChannelLatestUpdated(channelName)
                .orElseThrow(AssertionError::new);
        assertEquals(expectedLastItemUrl, channelLatestUpdated);
    }

    @Test
    void channelWithItemsOutsideSpokeWindow_hasStateEqualToChannelLatest() {
        // create channel with some items outside spoke ttl
        // verify channel/latest == last item key
        // verify ZK ChannelLatestUpdated == last item
    }

/*
set ttl to 2 minutes if necessary

make N channels

add items to N1 channels

do not add items to N2
delete N3 items from N4 of N channels
*/
}
