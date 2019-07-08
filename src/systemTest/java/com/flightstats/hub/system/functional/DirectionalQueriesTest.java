package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.model.TimeQueryResult;
import com.flightstats.hub.system.extension.TestClassWrapper;
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

class DirectionalQueriesTest extends TestClassWrapper {
    private String historicalChannelName;
    private DateTime mutableTimeEnd = DateTime.now().minusDays(1);
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

        insertedItems = insertHistoricalItems(10, itemNumber -> mutableTimeEnd.minusMinutes(10 - itemNumber));

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
        ChannelItemWithBody currentItem = getNthItem(3);

        Object actualNext = itemRetriever.getDirectionalItem(currentItem.getUrl(), next)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody expectedNext = getNthItem(4);
        assertEquals(expectedNext.getBody(), actualNext);
    }

    @Test
    void nextN_returnsNItemsAfterRequested() {
        ChannelItemWithBody currentItem = getNthItem(2);
        List<String> expectedNext = getItemsInRange(3, 4).stream().map(ChannelItemWithBody::getUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(currentItem.getUrl(), next, 2)
                .orElseThrow(AssertionError::new);

        assertEquals(expectedNext, Lists.newArrayList(actualItems.get_links().getUris()));
    }

    @Test
    void previous_returnsPreviousItem() {
        ChannelItemWithBody currentItem = getNthItem(6);

        Object actualNext = itemRetriever.getDirectionalItem(currentItem.getUrl(), previous)
                .orElseThrow(AssertionError::new);

        ChannelItemWithBody expectedNext = getNthItem(5);
        assertEquals(expectedNext.getBody(), actualNext);
    }

    @Test
    void previousN_returnsNItemsBeforeRequested() {
        ChannelItemWithBody currentItem = getNthItem(8);
        List<String> expectedNext = getItemsInRange(6, 7).stream().map(ChannelItemWithBody::getUrl).collect(toList());

        TimeQueryResult actualItems = itemRetriever.getDirectionalItems(currentItem.getUrl(), previous, 2)
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

    private List<ChannelItemWithBody> getItemsInRange(int firstItemNumber, int lastItemNumber) {
        ChannelItemWithBody firstItem = getNthItem(firstItemNumber);
        ChannelItemWithBody lastItem = getNthItem(lastItemNumber+1);
        return new ArrayList<>(insertedItems.subSet(firstItem, lastItem));
    }
}
