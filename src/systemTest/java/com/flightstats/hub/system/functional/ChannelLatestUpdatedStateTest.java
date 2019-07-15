package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelItemPathParts;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.InternalZookeeperService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

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

    @BeforeEach
    void setup() {
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
        List<ChannelItemWithBody> channelItems = channelItemCreator.addItems(channelName, "data", 3);
        ChannelItemWithBody expectedLastItem = channelItems.stream().min((other, item) -> item.getDateTime().compareTo(other.getDateTime()))
                .orElseThrow(AssertionError::new);

        String latestItemUrl = channelItemRetriever.getLatestUnstableItemUrl(channelName)
                .orElseThrow(AssertionError::new);
        assertEquals(expectedLastItem.getItemUrl(), latestItemUrl);

        await().atMost(90, TimeUnit.SECONDS)
                .until(() -> zookeeperService.getChannelLatestUpdated(channelName)
                                .orElseThrow(AssertionError::new),
                        is(equalTo(expectedLastItem.getZookeeperItemPath())));
    }

    @Test
    void channelWithItemsOutsideSpokeWindow_hasStateEqualToChannelLatest() {
        DateTime now = DateTime.now();
        String historicalChannelName = channelName + "-historical";
        ChannelConfig config = ChannelConfig.builder()
                .name(historicalChannelName)
                .mutableTime(now)
                .build();
        channelConfigService.create(config);

        SortedSet<ChannelItemWithBody> channelItems = channelItemCreator.addHistoricalItems(historicalChannelName,
                Arrays.asList(now.minusMinutes(5), now.minusMinutes(4)));
        ChannelItemWithBody mostRecentItem = channelItems.last();

        ChannelConfig updatedConfig = config.toBuilder()
                .mutableTime(now.minusDays(1))
                .build();
        channelConfigService.update(updatedConfig);

        String latestItemUrl = channelItemRetriever.getLatestUnstableItemUrl(historicalChannelName)
                .orElseThrow(AssertionError::new);
        assertEquals(mostRecentItem.getItemUrl(), latestItemUrl);

        await().atMost(90, TimeUnit.SECONDS)
                .until(() -> zookeeperService.getChannelLatestUpdated(historicalChannelName)
                                .orElseThrow(AssertionError::new),
                        is(equalTo(mostRecentItem.getZookeeperItemPath())));
    }
}
