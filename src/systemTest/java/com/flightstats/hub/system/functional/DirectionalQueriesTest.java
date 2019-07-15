package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.model.TimeQueryResult;
import com.flightstats.hub.system.extension.TestSuiteClassWrapper;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelItemRetriever;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.flightstats.hub.model.ChannelItemQueryDirection.next;
import static com.flightstats.hub.model.ChannelItemQueryDirection.previous;
import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionalQueriesTest extends TestSuiteClassWrapper {
    private String historicalChannelName;
    private DateTime mutableTimeEnd = DateTime.parse("2016-04-15T12:00:10.000Z");
    private SortedSet<ChannelItemWithBody> insertedItems;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemRetriever itemRetriever;
    @Inject
    private ChannelItemCreator itemCreator;

    @BeforeAll
    void before() {
        historicalChannelName = randomAlphaNumeric(10);
        ChannelConfig config = ChannelConfig.builder()
                .name(historicalChannelName)
                .mutableTime(mutableTimeEnd)
                .build();
        channelConfigService.create(config);

        insertedItems = insertHistoricalItems(10, itemNumber -> mutableTimeEnd.minusSeconds(10 - itemNumber));

        ChannelConfig updatedConfig = config.toBuilder()
                .mutableTime(mutableTimeEnd.minusDays(11))
                .build();
        channelConfigService.update(updatedConfig);
    }

    @AfterAll
    void cleanup() {
        channelConfigService.delete(historicalChannelName);
    }

    private SortedSet<ChannelItemWithBody> insertHistoricalItems(Integer numberOfItems, Function<Integer, DateTime> insertionDateCalculation) {
        List<DateTime> historicalItemTimes = IntStream.range(1, numberOfItems)
                .mapToObj(insertionDateCalculation::apply)
                .collect(toList());

        return itemCreator.addHistoricalItems(historicalChannelName, historicalItemTimes);
    }

    @Test
    void next_returnsNextItem() {
        String startingUrl = getNthItemUrl(3, ChannelItemWithBody::getItemUrl);

        Object actualNext = itemRetriever.getDirectionalItem(startingUrl, next)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody expectedNext = getNthItem(4);
        assertEquals(expectedNext.getBody(), actualNext);
    }

    @Test
    void nextN_returnsNItemsAfterRequested() {
        String startingUrl = getNthItemUrl(2, ChannelItemWithBody::getItemUrl);
        List<String> expectedNext = getItemsInRange(3, 4).stream().map(ChannelItemWithBody::getItemUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(startingUrl, next, 2)
                .orElseThrow(AssertionError::new);

        assertEquals(expectedNext, Lists.newArrayList(actualItems.get_links().getUris()));
    }

    @Test
    void nextN_returnsNSecondsIncludingRequestedSecond() {
        String startingUrl = getNthItemUrl(2, ChannelItemWithBody::getSecondUrl);
        List<String> expectedNext = getItemsInRange(2, 3).stream().map(ChannelItemWithBody::getSecondUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(startingUrl, next, 2)
                .orElseThrow(AssertionError::new);

        assertEquals(expectedNext, Lists.newArrayList(actualItems.get_links().getUris()));
    }

    @Test
    void previous_returnsPreviousItem() {
        String startingUrl = getNthItemUrl(6, ChannelItemWithBody::getItemUrl);

        Object actualNext = itemRetriever.getDirectionalItem(startingUrl, previous)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody expectedNext = getNthItem(5);
        assertEquals(expectedNext.getBody(), actualNext);
    }

    @Test
    void previousN_returnsNItemsBeforeRequested() {
        String startingUrl = getNthItemUrl(8, ChannelItemWithBody::getItemUrl);
        List<String> expectedNext = getItemsInRange(6, 7).stream().map(ChannelItemWithBody::getItemUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(startingUrl, previous, 2)
                .orElseThrow(AssertionError::new);

        assertEquals(expectedNext, Lists.newArrayList(actualItems.get_links().getUris()));
    }

    @Test
    void previousN_returnsNSecondsBeforeRequestedSecond() {
        String startingUrl = getNthItemUrl(8, ChannelItemWithBody::getSecondUrl);
        List<String> expectedNext = getItemsInRange(6, 7).stream().map(ChannelItemWithBody::getSecondUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(startingUrl, previous, 2)
                .orElseThrow(AssertionError::new);

        assertEquals(expectedNext, Lists.newArrayList(actualItems.get_links().getUris()));
    }

    @Test
    void earliest_returnsFirstItemOnChannel() {
        Object actualNext = itemRetriever.getEarliestItem(historicalChannelName)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody firstItem = insertedItems.first();
        assertEquals(firstItem.getBody(), actualNext);
    }

    @Test
    void latest_returnsMostRecentItemOnChannel() {
        Object actualNext = itemRetriever.getLatestItem(historicalChannelName)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody lastItem = insertedItems.last();
        assertEquals(lastItem.getBody(), actualNext);
    }

    private ChannelItemWithBody getNthItem(int itemNumber) {
        return new ArrayList<>(insertedItems).get(itemNumber - 1);
    }

    private String getNthItemUrl(int itemNumber, Function<ChannelItemWithBody, String> urlRetriever) {
        return urlRetriever.apply(getNthItem(itemNumber));
    }

    private List<ChannelItemWithBody> getItemsInRange(int firstItemNumber, int lastItemNumber) {
        ChannelItemWithBody firstItem = getNthItem(firstItemNumber);
        ChannelItemWithBody lastItem = getNthItem(lastItemNumber+1);
        return new ArrayList<>(insertedItems.subSet(firstItem, lastItem));
    }
}
