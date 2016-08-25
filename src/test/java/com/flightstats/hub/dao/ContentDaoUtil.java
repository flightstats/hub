package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.*;

public class ContentDaoUtil {

    private final static Logger logger = LoggerFactory.getLogger(ContentDaoUtil.class);

    private ContentDao contentDao;

    public ContentDaoUtil(ContentDao contentDao) {
        this.contentDao = contentDao;
    }

    public void testWriteRead() throws Exception {
        String channel = "testWriteRead";
        Content content = createContent();
        ContentKey key = contentDao.insert(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.get(channel, key);
        compare(content, read, key.toString().getBytes());
    }

    public void testWriteReadNoOptionals() throws Exception {
        String channel = "testWriteReadNoOptionals";
        byte[] data = "testWriteReadNoOptionals data".getBytes();
        Content content = Content.builder()
                .withContentKey(new ContentKey())
                .withData(data)
                .build();
        ContentKey key = contentDao.insert(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.get(channel, key);
        compare(content, read, data);
    }

    public void testQueryRangeDay() throws Exception {
        String channel = "testQueryRangeDay" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 0, 0, DateTimeZone.UTC);
        for (int i = 0; i < 24; i += 3) {
            ContentKey key = new ContentKey(start.plusHours(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            contentDao.insert(channel, content);
        }
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(TimeUtil.Unit.DAYS)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testQueryRangeHour() throws Exception {
        String channel = "testQueryRangeHour" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 14, 0, DateTimeZone.UTC);
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusMinutes(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            contentDao.insert(channel, content);
        }
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(TimeUtil.Unit.HOURS)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testQueryRangeMinute() throws Exception {
        String channel = "testQueryRangeMinute" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 15, 27, DateTimeZone.UTC);
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusSeconds(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            contentDao.insert(channel, content);
        }
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testQuery15Minutes() throws Exception {
        String channel = "testQuery15Minutes" + RandomStringUtils.randomAlphanumeric(20);
        DateTime start = new DateTime(2014, 11, 14, 14, 0, DateTimeZone.UTC);

        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .endTime(start.plusMinutes(19))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(0, found.size());

        List<ContentKey> keys = new ArrayList<>();

        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusMinutes(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            contentDao.insert(channel, content);
        }
        timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .endTime(start.plusMinutes(19))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        found = contentDao.queryByTime(timeQuery);
        assertEquals(4, found.size());

        timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start.plusHours(2))
                .endTime(start.plusHours(2).plusMinutes(19))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        found = contentDao.queryByTime(timeQuery);
        assertEquals(0, found.size());

    }

    public void testDirectionQueryTTL() throws Exception {
        String channel = "testDirectionQueryTTL" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 20, 3, true, start.minusHours(2));
        query(channel, keys, 20, 4, true, start.minusHours(4));
        query(channel, keys, 20, 4, true, start.minusHours(5));
        query(channel, keys, 20, 4, false, start.plusMinutes(1));
        query(channel, keys, 1, 1, true, start.minusDays(10));
    }

    public void testDirectionQuery() throws Exception {
        String channel = "testDirectionQuery" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusDays(i), "B" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 20, 4, true, start.minusHours(2));
        query(channel, keys, 20, 6, true, start.minusHours(4));
        query(channel, keys, 20, 13, true, start.minusDays(5));
        query(channel, keys, 20, 14, false, start.plusMinutes(1));
        query(channel, keys, 5, 5, false, start);
        query(channel, keys, 20, 5, false, start.minusDays(1));
    }

    public void testEarliest() throws Exception {
        String channel = "testEarliest" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 60; i++) {
            ContentKey key = new ContentKey(start.minusMinutes(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 60, 60, true, start.minusHours(1));
        query(channel, keys, 60, 16, true, start.minusMinutes(15));
        query(channel, keys, 60, 60, true, start.minusDays(20));
    }

    public void testDeleteMaxItems() throws Exception {
        String channel = "testDeleteMaxItems" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 5; i++) {
            ContentKey key = new ContentKey(start.minusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 5, 5, true, start.minusDays(1));
        contentDao.deleteBefore(channel, keys.get(2));
        query(channel, keys, 5, 3, true, start.minusDays(1));
        contentDao.deleteBefore(channel, keys.get(0));
        query(channel, keys, 5, 1, true, start.minusDays(1));
    }

    private void query(String channel, List<ContentKey> keys,
                       int count, int expected, boolean next, DateTime queryTime) {
        ActiveTraces.start("query ", channel, count, queryTime);
        DirectionQuery query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .count(count)
                .next(next)
                .contentKey(new ContentKey(queryTime, "0"))
                .ttlTime(TimeUtil.now().minusDays(120))
                .liveChannel(true)
                .channelStable(TimeUtil.now())
                .build();
        Collection<ContentKey> found = contentDao.query(query);
        logger.info("query {} {}", queryTime, found);
        ActiveTraces.getLocal().log(logger);
        assertEquals(expected, found.size());
        assertTrue(keys.containsAll(found));
    }

    public static Content createContent(ContentKey key) {
        return Content.builder()
                .withContentKey(key)
                .withContentType("stuff")
                .withData(key.toString().getBytes())
                .build();
    }

    public static Content createContent() {
        return createContent(new ContentKey());
    }

    public static void compare(Content content, Content read, byte[] expected) {
        assertEquals(content.getContentKey().get(), read.getContentKey().get());
        assertEquals(content.getContentType(), read.getContentType());
        byte[] data = read.getData();
        assertArrayEquals(expected, data);
        assertEquals(content, read);
    }

    public void testBulkWrite() throws Exception {
        String channel = "testBulkWrite";
        BulkContent bulkContent = BulkContent.builder()
                .channel(channel)
                .isNew(false)
                .build();

        DateTime time = new DateTime();
        for (int i = 0; i < 10; i++) {
            time = time.plusMillis(1);
            ContentKey key = new ContentKey(time);
            Content content = Content.builder()
                    .withContentKey(key)
                    .withContentType("text/plain")
                    .withData((key.toUrl()).getBytes())
                    .build();
            bulkContent.getItems().add(content);
        }
        SortedSet<ContentKey> contentKeys = contentDao.insert(bulkContent);
        assertEquals(10, contentKeys.size());

        List<Content> items = bulkContent.getItems();
        for (Content item : items) {
            ContentKey contentKey = item.getContentKey().get();
            Content found = contentDao.get(channel, contentKey);
            compare(item, found, contentKey.toUrl().getBytes());

        }

    }

    public void testEmptyQuery() throws Exception {
        String channel = "testEmptyQuery" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now().minusHours(10);
        query(channel, keys, 1, 0, false, start);


    }
}
