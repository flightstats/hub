package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContentDaoUtil {

    private final static Logger logger = LoggerFactory.getLogger(ContentDaoUtil.class);

    private ContentDao contentDao;

    public ContentDaoUtil(ContentDao contentDao) {
        this.contentDao = contentDao;
    }

    public void testWriteRead(Content content) throws Exception {
        String channel = "testWriteRead";
        content.packageStream();
        ContentKey key = contentDao.insert(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.get(channel, key);
        compare(content, read, key.toString().getBytes());
    }

    public void testWriteHistorical() throws Exception {
        String data = StringUtils.randomAlphaNumeric(2048);
        String channel = "testWriteHistorical";
        ContentKey contentKey = new ContentKey();
        Content content = Content.builder().withContentType("contentIsThis")
                .withContentKey(contentKey)
                .withStream(new ByteArrayInputStream(data.getBytes()))
                .build();
        content.packageStream();
        ContentKey historical = contentDao.insertHistorical(channel, content);
        assertEquals(contentKey, historical);
        Content found = contentDao.get(channel, contentKey);
        assertNotNull(found);
        assertEquals(content.getContentType().get(), found.getContentType().get());
        assertArrayEquals(data.getBytes(), found.getData());
    }

    public void testQueryRangeDay() throws Exception {
        String channel = "testQueryRangeDay" + StringUtils.randomAlphaNumeric(20);
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
        String channel = "testQueryRangeHour" + StringUtils.randomAlphaNumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 14, 0, DateTimeZone.UTC);
        create10ItemsBy6Minutes(channel, start, keys);
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(TimeUtil.Unit.HOURS)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testQueryRangeMinute() throws Exception {
        String channel = "testQueryRangeMinute" + StringUtils.randomAlphaNumeric(20);
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
        String channel = "testQuery15Minutes" + StringUtils.randomAlphaNumeric(20);
        DateTime start = new DateTime(2014, 11, 14, 14, 0, DateTimeZone.UTC);

        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .limitKey(ContentKey.lastKey(start.plusMinutes(19)))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        Collection<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(0, found.size());

        List<ContentKey> keys = new ArrayList<>();

        create10ItemsBy6Minutes(channel, start, keys);
        timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .limitKey(ContentKey.lastKey(start.plusMinutes(19)))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        found = contentDao.queryByTime(timeQuery);
        assertEquals(4, found.size());

        timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start.plusHours(2))
                .limitKey(ContentKey.lastKey(start.plusHours(2).plusMinutes(19)))
                .unit(TimeUtil.Unit.MINUTES)
                .build();
        found = contentDao.queryByTime(timeQuery);
        assertEquals(0, found.size());

    }

    private void create10ItemsBy6Minutes(String channel, DateTime start, List<ContentKey> keys) throws Exception {
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusMinutes(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            contentDao.insert(channel, content);
        }
    }


    public void testPreviousFromBulk_Issue753() throws Exception {
        String channel = "testPreviousFromBulk_Issue753" + StringUtils.randomAlphaNumeric(20);
        LinkedList<ContentKey> keys = new LinkedList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start, "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
        query(channel, keys, 6, 6, false, keys.getLast());
    }

    public void testDirectionQueryTTL() throws Exception {
        String channel = "testDirectionQueryTTL" + StringUtils.randomAlphaNumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        create7Items(channel, keys, start);
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 20, 3, true, start.minusHours(2));
        query(channel, keys, 20, 4, true, start.minusHours(4));
        query(channel, keys, 20, 4, true, start.minusHours(5));
        query(channel, keys, 20, 6, false, start.plusMinutes(1));
        query(channel, keys, 1, 1, true, start.minusDays(10));
    }

    private void create7Items(String channel, List<ContentKey> keys, DateTime start) throws Exception {
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.insert(channel, createContent(key));
        }
    }

    public void testDirectionQuery() throws Exception {
        String channel = "testDirectionQuery" + StringUtils.randomAlphaNumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        create7Items(channel, keys, start);
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
        String channel = "testEarliest" + StringUtils.randomAlphaNumeric(20);
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
        String channel = "testDeleteMaxItems" + StringUtils.randomAlphaNumeric(20);
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
        query(channel, keys, count, expected, next, new ContentKey(queryTime, "0"));
    }

    private void query(String channel, List<ContentKey> keys, int count, int expected, boolean next, ContentKey startKey) {
        ActiveTraces.start("query ", channel, count, startKey);
        ChannelConfig channelConfig = ChannelConfig.builder().name(channel).build();
        DirectionQuery query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .channelConfig(channelConfig)
                .count(count)
                .next(next)
                .startKey(startKey)
                .earliestTime(TimeUtil.now().minusDays((int) channelConfig.getTtlDays()))
                .channelStable(TimeUtil.now())
                .build();
        Collection<ContentKey> found = contentDao.query(query);
        logger.info("startKey {}", startKey);
        logger.info("keys {}", keys);
        logger.info("found {}", found);
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
        String channel = "testEmptyQuery" + StringUtils.randomAlphaNumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now().minusHours(10);
        query(channel, keys, 1, 0, false, start);


    }
}
