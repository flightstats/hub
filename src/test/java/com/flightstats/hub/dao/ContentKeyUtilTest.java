package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentKeyUtilTest {

    @Test
    public void testConvertKeysToMinutes() {
        DateTime start = TimeUtil.now();
        SortedSet<ContentKey> keys = new TreeSet<>();
        for (int i = 0; i < 100; i += 2) {
            keys.add(new ContentKey(start.plusMinutes(i), "A" + i));
            keys.add(new ContentKey(start.plusMinutes(i), "B" + i));
        }
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        assertEquals(50, minutePaths.size());
        for (MinutePath minutePath : minutePaths) {
            assertEquals(2, minutePath.getKeys().size());
            for (ContentKey key : minutePath.getKeys()) {
                assertEquals(minutePath.getTime(), new MinutePath(key.getTime()).getTime());
            }
        }
    }

    @Test
    public void testFilterStartKey() throws Exception {
        ContentKey startKey = new ContentKey(new DateTime().minusDays(60), "B");
        List<ContentKey> items = getEpochItems(startKey);
        DirectionQuery query = DirectionQuery.builder()
                .channelName("testFilterNext")
                .channelConfig(ChannelConfig.builder().name("testFilterNext").build())
                .next(true)
                .count(4)
                .startKey(startKey)
                .build();
        SortedSet<ContentKey> fourCount = ContentKeyUtil.filter(items, query);
        assertEquals(4, fourCount.size());
        assertTrue(fourCount.containsAll(items.subList(5, 9)));

        SortedSet<ContentKey> twoCount = ContentKeyUtil.filter(items, query.withCount(2));
        assertEquals(2, twoCount.size());
        assertTrue(twoCount.containsAll(items.subList(5, 7)));

        SortedSet<ContentKey> previousFour = ContentKeyUtil.filter(items, query.withNext(false));
        assertEquals(4, previousFour.size());
        assertTrue(previousFour.containsAll(items.subList(0, 4)));

        SortedSet<ContentKey> previousTwo = ContentKeyUtil.filter(items, query.withCount(2).withNext(false));
        assertEquals(2, previousTwo.size());
        assertTrue(previousTwo.containsAll(items.subList(2, 4)));
    }

    @Test
    public void testFilterStable() throws Exception {
        DateTime stable = TimeUtil.stable();
        ContentKey startKey = new ContentKey(stable.minusSeconds(5), "B");

        List<ContentKey> items = new ArrayList<>();
        items.add(new ContentKey(startKey.getTime().minusMillis(3)));
        items.add(new ContentKey(startKey.getTime().minusMillis(2)));
        items.add(new ContentKey(startKey.getTime().minusMillis(1)));
        items.add(new ContentKey(startKey.getTime(), "A"));
        items.add(startKey);
        items.add(new ContentKey(startKey.getTime().plusSeconds(1)));
        items.add(new ContentKey(startKey.getTime().plusSeconds(3)));
        items.add(new ContentKey(startKey.getTime().plusSeconds(4)));
        items.add(new ContentKey(startKey.getTime().plusSeconds(4).plusMillis(999)));
        items.add(new ContentKey(startKey.getTime().plusSeconds(5).plusMillis(1)));
        items.add(new ContentKey(startKey.getTime().plusSeconds(6)));

        DirectionQuery query = DirectionQuery.builder()
                .channelName("testFilterStable")
                .channelConfig(ChannelConfig.builder().name("testFilterStable").build())
                .next(true)
                .count(10)
                .startKey(startKey)
                .stable(true)
                .channelStable(stable)
                .build();
        SortedSet<ContentKey> fourCount = ContentKeyUtil.filter(items, query);
        assertEquals(4, fourCount.size());
        assertTrue(fourCount.containsAll(items.subList(5, 9)));

        SortedSet<ContentKey> previousTwo = ContentKeyUtil.filter(items, query.withStable(false));
        assertEquals(6, previousTwo.size());
        assertTrue(previousTwo.containsAll(items.subList(5, 11)));
    }

    @Test
    public void testFilterNextEpoch() throws Exception {
        ContentKey mutableItem = new ContentKey(new DateTime(2016, 12, 7, 17, 8), "B");
        List<ContentKey> items = getEpochItems(mutableItem);
        DirectionQuery query = buildEpochQuery(mutableItem, "testFilterNextEpoch", true);

        SortedSet<ContentKey> immutable = ContentKeyUtil.filter(items, query);
        assertEquals(3, immutable.size());
        assertTrue(immutable.containsAll(items.subList(6, 9)));

        DirectionQuery mutableQuery = query.withEpoch(Epoch.MUTABLE);
        SortedSet<ContentKey> mutable = ContentKeyUtil.filter(items, mutableQuery);
        assertEquals(1, mutable.size());
        assertTrue(mutable.containsAll(items.subList(5, 6)));

        DirectionQuery earlyStartQuery = mutableQuery.withStartKey(new ContentKey(mutableItem.getTime().minusDays(1)));
        SortedSet<ContentKey> mutableEarlier = ContentKeyUtil.filter(items, earlyStartQuery);
        assertEquals(6, mutableEarlier.size());
        assertTrue(mutableEarlier.containsAll(items.subList(0, 6)));

        DirectionQuery earlyStartAllQuery = earlyStartQuery.withEpoch(Epoch.ALL);
        SortedSet<ContentKey> all = ContentKeyUtil.filter(items, earlyStartAllQuery);
        assertEquals(9, all.size());
        assertTrue(all.containsAll(items));
    }

    @Test
    public void testFilterPreviousEpoch() throws Exception {
        ContentKey mutableItem = new ContentKey(new DateTime(2016, 12, 7, 17, 8), "B");
        List<ContentKey> items = getEpochItems(mutableItem);
        DirectionQuery query = buildEpochQuery(mutableItem, "testFilterPreviousEpoch", false);

        SortedSet<ContentKey> immutable = ContentKeyUtil.filter(items, query);
        assertEquals(0, immutable.size());

        DirectionQuery mutableQuery = query.withEpoch(Epoch.MUTABLE);
        SortedSet<ContentKey> mutable = ContentKeyUtil.filter(items, mutableQuery);
        assertEquals(4, mutable.size());
        assertTrue(mutable.containsAll(items.subList(0, 4)));

        DirectionQuery laterStartQuery = mutableQuery.withStartKey(new ContentKey(mutableItem.getTime().plusDays(1)));
        SortedSet<ContentKey> mutableLater = ContentKeyUtil.filter(items, laterStartQuery);
        assertEquals(6, mutableLater.size());
        assertTrue(mutableLater.containsAll(items.subList(0, 6)));

        DirectionQuery laterStartAllQuery = laterStartQuery.withEpoch(Epoch.ALL);
        SortedSet<ContentKey> all = ContentKeyUtil.filter(items, laterStartAllQuery);
        assertEquals(9, all.size());
        assertTrue(all.containsAll(items));
    }

    private List<ContentKey> getEpochItems(ContentKey mutableItem) {
        List<ContentKey> items = new ArrayList<>();
        items.add(new ContentKey(mutableItem.getTime().minusMillis(3)));
        items.add(new ContentKey(mutableItem.getTime().minusMillis(2)));
        items.add(new ContentKey(mutableItem.getTime().minusMillis(1)));
        items.add(new ContentKey(mutableItem.getTime(), "A"));
        items.add(mutableItem);
        items.add(new ContentKey(mutableItem.getTime(), "C"));
        items.add(new ContentKey(mutableItem.getTime().plusMillis(1)));
        items.add(new ContentKey(mutableItem.getTime().plusMillis(2)));
        items.add(new ContentKey(mutableItem.getTime().plusMillis(3)));
        return items;
    }

    private DirectionQuery buildEpochQuery(ContentKey mutableItem, String name, boolean next) {
        ChannelConfig channelConfig = ChannelConfig.builder()
                .name(name)
                .mutableTime(mutableItem.getTime())
                .build();
        return DirectionQuery.builder()
                .channelName(name)
                .channelConfig(channelConfig)
                .next(next)
                .count(10)
                .startKey(mutableItem)
                .epoch(Epoch.IMMUTABLE)
                .build();
    }



}