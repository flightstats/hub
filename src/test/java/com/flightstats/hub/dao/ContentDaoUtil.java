package com.flightstats.hub.dao;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TracesImpl;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        ContentKey key = contentDao.write(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.read(channel, key);
        compare(content, read, key.toString().getBytes());
    }

    public void testWriteReadNoOptionals() throws Exception {
        String channel = "testWriteReadNoOptionals";
        byte[] data = "testWriteReadNoOptionals data".getBytes();
        Content content = Content.builder()
                .withContentKey(new ContentKey())
                .withData(data)
                .build();
        ContentKey key = contentDao.write(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.read(channel, key);
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
            contentDao.write(channel, content);
        }
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.DAYS, new TracesImpl());
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
            contentDao.write(channel, content);
        }
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.HOURS, new TracesImpl());
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
            contentDao.write(channel, content);
        }
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.MINUTES, new TracesImpl());
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testDirectionQueryTTL() throws Exception {
        String channel = "testDirectionQueryTTL" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = TimeUtil.now();
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.write(channel, createContent(key));
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
            contentDao.write(channel, createContent(key));
        }
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.minusDays(i), "B" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.write(channel, createContent(key));
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
            contentDao.write(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 60, 60, true, start.minusHours(1));
        query(channel, keys, 60, 16, true, start.minusMinutes(15));
        query(channel, keys, 60, 60, true, start.minusDays(20));
    }

    private void query(String channel, List<ContentKey> keys,
                       int count, int expected, boolean next, DateTime queryTime) {
        DirectionQuery query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .count(count)
                .next(next)
                .contentKey(new ContentKey(queryTime, "0"))
                .traces(new TracesImpl())
                .ttlDays(10)
                .build();
        Collection<ContentKey> found = contentDao.query(query);
        logger.info("query {} {}", queryTime, found);
        query.getTraces().log(logger);
        assertEquals(expected, found.size());
        assertTrue(keys.containsAll(found));
    }

    private Content createContent(ContentKey key) {
        return Content.builder()
                .withContentKey(key)
                .withContentLanguage("lang")
                .withContentType("stuff")
                .withUser("bob")
                .withData(key.toString().getBytes())
                .build();
    }

    private Content createContent() {
        return createContent(new ContentKey());
    }

    private void compare(Content content, Content read, byte[] data) {
        assertEquals(content.getContentKey().get(), read.getContentKey().get());
        assertEquals(content.getContentLanguage(), read.getContentLanguage());
        assertEquals(content.getContentType(), read.getContentType());
        assertEquals(content.getUser(), read.getUser());
        assertArrayEquals(data, read.getData());
        assertEquals(content, read);
    }
}
