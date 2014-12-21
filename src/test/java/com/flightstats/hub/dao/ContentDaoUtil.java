package com.flightstats.hub.dao;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
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
        compare(content, read);
    }

    public void testWriteReadNoOptionals() throws Exception {
        String channel = "testWriteReadNoOptionals";
        Content content = Content.builder()
                .withContentKey(new ContentKey())
                .withData("testWriteReadNoOptionals data".getBytes())
                .build();
        ContentKey key = contentDao.write(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = contentDao.read(channel, key);
        compare(content, read);
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
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.DAYS);
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
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.HOURS);
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
        Collection<ContentKey> found = contentDao.queryByTime(channel, start, TimeUtil.Unit.MINUTES);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    public void testDirectionQuery() throws Exception {
        String channel = "testDirectionQuery" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 22, 27, DateTimeZone.UTC);
        for (int i = 0; i < 7; i++) {
            ContentKey key = new ContentKey(start.plusHours(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            contentDao.write(channel, createContent(key));
        }
        logger.info("wrote {} {}", keys.size(), keys);
        query(channel, keys, 20, 7, true, start.minusMinutes(1));
        query(channel, keys, 3, 3, true, start.minusMinutes(1));
        query(channel, keys, 20, 7, false, start.plusHours(8));
        //query(channel, keys, 3, 3, false, start.plusHours(8));
    }

    private void query(String channel, List<ContentKey> keys,
                       int count, int expected, boolean next, DateTime queryTime) {
        DirectionQuery query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .count(count)
                .next(next)
                .contentKey(new ContentKey(queryTime, "blah"))
                .build();
        Collection<ContentKey> found = contentDao.query(query);
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

    private void compare(Content content, Content read) {
        assertEquals(content.getContentKey().get(), read.getContentKey().get());
        assertEquals(content.getContentLanguage(), read.getContentLanguage());
        assertEquals(content.getContentType(), read.getContentType());
        assertEquals(content.getUser(), read.getUser());
        assertArrayEquals(content.getData(), read.getData());
        assertEquals(content, read);
    }
}
