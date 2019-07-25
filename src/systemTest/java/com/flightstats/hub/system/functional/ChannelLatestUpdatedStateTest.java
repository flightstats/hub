package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
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
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

class ChannelLatestUpdatedStateTest extends TestClassWrapper {
    private static final String NO_CHANNEL_LATEST_ZK_VALUE = "1970/01/01/00/00/00/001/none";
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
    }

    @AfterEach
    void cleanup() {
        channelConfigService.delete(channelName);
    }

    @Test
    void channelWithNoItem_hasStateValueOfEpochNone_and_hasNoChannelLatest() {
        channelConfigService.createWithDefaults(channelName);

        assertChannelLatestUrlIsBlank();
        assertZookeeperLatestUpdatedItemPathEquals(NO_CHANNEL_LATEST_ZK_VALUE);
    }

    @Test
    void channelWithItemsInSpoke_hasNoZkLatestButReturnsLatestInEndpoint() {
        channelConfigService.createWithDefaults(channelName);
        List<ChannelItemWithBody> channelItems = channelItemCreator.addItems(channelName, "data", 3);
        ChannelItemWithBody expectedLastItem = channelItems.stream()
                .min((other, item) -> item.getDateTime().compareTo(other.getDateTime()))
                .orElseThrow(AssertionError::new);

        assertChannelLatestUrlEquals(expectedLastItem.getItemUrl());
        assertZookeeperLatestUpdatedIsEmpty();
    }

    @Test
    void historicalChannelWithItemsInTheMutabilityWindow_showsUpAsHavingNoZkLatestOrChannelLatest() {
        DateTime now = DateTime.now();
        ChannelConfig config = ChannelConfig.builder()
                .name(channelName)
                .mutableTime(now)
                .build();
        channelConfigService.create(config);

        channelItemCreator.addHistoricalItems(channelName,
                Arrays.asList(now.minusMinutes(20), now.minusMinutes(21)));

        assertChannelLatestUrlIsBlank();
        assertZookeeperLatestUpdatedItemPathEquals(NO_CHANNEL_LATEST_ZK_VALUE);

    }

    @Test
    void channelWithItemsOutsideSpokeWindow_hasStateEqualToChannelLatest() {
        DateTime now = DateTime.now();
        ChannelConfig config = ChannelConfig.builder()
                .name(channelName)
                .mutableTime(now)
                .build();
        channelConfigService.create(config);

        SortedSet<ChannelItemWithBody> channelItems = channelItemCreator.addHistoricalItems(channelName,
                Arrays.asList(now.minusMinutes(9).minusSeconds(45), now.minusMinutes(9).minusSeconds(44)));
        ChannelItemWithBody mostRecentItem = channelItems.last();

        ChannelConfig updatedConfig = ChannelConfig.builder()
                .name(channelName)
                .ttlDays(5)
                .build();
        channelConfigService.updateExpirationSettings(updatedConfig);

        assertChannelLatestUrlEquals(mostRecentItem, true);
        // TODO: this never gets set because it was inserted as an historical item. We should decrease spoke TTL and wait 'til it passes. like a chump.
        assertZookeeperLatestUpdatedItemPathEquals(NO_CHANNEL_LATEST_ZK_VALUE);
    }

    @Test
    void channelWithOnlyExpiredItems_hasNoLatest() {
        DateTime now = DateTime.now();
        ChannelConfig config = ChannelConfig.builder()
                .name(channelName)
                .mutableTime(now)
                .build();
        channelConfigService.create(config);

        channelItemCreator.addHistoricalItems(channelName,
                Arrays.asList(now.minusDays(3), now.minusDays(3)));

        ChannelConfig updatedConfig = ChannelConfig.builder()
                .name(channelName)
                .ttlDays(1)
                .build();
        channelConfigService.updateExpirationSettings(updatedConfig);

        assertChannelLatestUrlIsBlank(true);
        assertZookeeperLatestUpdatedItemPathEquals(NO_CHANNEL_LATEST_ZK_VALUE);

    }

    private void assertChannelLatestUrlEquals(ChannelItemWithBody expectedItem, boolean mutable) {
        assertChannelLatestUrlEquals(expectedItem.getItemUrl(), mutable);
    }

    private void assertChannelLatestUrlIsBlank() {
        assertChannelLatestUrlIsBlank(false);
    }

    private void assertChannelLatestUrlIsBlank(boolean mutable) {
        assertChannelLatestUrlEquals("", mutable);

    }

    private void assertChannelLatestUrlEquals(String latestUrl) {
        assertChannelLatestUrlEquals(latestUrl, false);
    }

    private void assertChannelLatestUrlEquals(String latestUrl, boolean mutable) {
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> channelItemRetriever.getLatestItemUrl(channelName, mutable).orElse(""),
                        is(equalTo(latestUrl)));
    }

    private void assertZookeeperLatestUpdatedEquals(ChannelItemWithBody expectedItem) {
        await().atMost(90, TimeUnit.SECONDS)
                .until(() -> zookeeperService.getChannelLatestUpdated(channelName),
                        is(equalTo(expectedItem.getZookeeperItemPath())));
    }
    private void assertZookeeperLatestUpdatedIsEmpty() {
        assertZookeeperLatestUpdatedItemPathEquals("");

    }

    private void assertZookeeperLatestUpdatedItemPathEquals(String expectedItemPath) {
        await().atMost(90, TimeUnit.SECONDS)
                .until(() -> zookeeperService.getChannelLatestUpdated(channelName).orElse(""),
                        is(equalTo(expectedItemPath)));
    }
}
