package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class S3ContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDaoTest.class);
    private static S3ContentDao s3ContentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AwsConnectorFactory factory = new AwsConnectorFactory("dynamodb.us-east-1.amazonaws.com", "s3-external-1.amazonaws.com", "HTTP", false);
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        s3ContentDao = new S3ContentDao(s3Client, false, bucketName, 3);
    }

    @Test
    public void testWriteRead() throws Exception {
        String channel = "testWriteRead";
        Content content = createContent();
        ContentKey key = s3ContentDao.write(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = s3ContentDao.read(channel, key);
        compare(content, read);
    }

    @Test
    public void testWriteReadNoOptionals() throws Exception {
        String channel = "testWriteReadNoOptionals";
        Content content = Content.builder()
                .withContentKey(new ContentKey())
                .withData("testWriteReadNoOptionals data".getBytes())
                .build();
        ContentKey key = s3ContentDao.write(channel, content);
        assertEquals(content.getContentKey().get(), key);
        Content read = s3ContentDao.read(channel, key);
        compare(content, read);
    }

    @Test
    public void testQueryRangeDay() throws Exception {
        String channel = "testQueryRangeDay" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 0, 0, DateTimeZone.UTC);
        for (int i = 0; i < 24; i += 3) {
            ContentKey key = new ContentKey(start.plusHours(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            s3ContentDao.write(channel, content);
        }
        Collection<ContentKey> found = s3ContentDao.queryByTime(channel, start, TimeUtil.Unit.DAYS);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    @Test
    public void testQueryRangeHour() throws Exception {
        String channel = "testQueryRangeHour" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 14, 0, DateTimeZone.UTC);
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusMinutes(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            s3ContentDao.write(channel, content);
        }
        Collection<ContentKey> found = s3ContentDao.queryByTime(channel, start, TimeUtil.Unit.HOURS);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    @Test
    public void testQueryRangeMinute() throws Exception {
        String channel = "testQueryRangeMinute" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 15, 27, DateTimeZone.UTC);
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusSeconds(i), "A" + i);
            keys.add(key);
            Content content = createContent(key);
            s3ContentDao.write(channel, content);
        }
        Collection<ContentKey> found = s3ContentDao.queryByTime(channel, start, TimeUtil.Unit.MINUTES);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));
    }

    @Test
    public void testQueryNext() throws Exception {
        String channel = "testQueryNext" + RandomStringUtils.randomAlphanumeric(20);
        List<ContentKey> keys = new ArrayList<>();
        DateTime start = new DateTime(2014, 11, 14, 15, 27, DateTimeZone.UTC);
        for (int i = 0; i < 60; i += 6) {
            ContentKey key = new ContentKey(start.plusMinutes(i), "A" + i);
            keys.add(key);
            logger.info("writing " + key);
            Content content = createContent(key);
            s3ContentDao.write(channel, content);
        }
        logger.info("wrote {} {}", keys.size(), keys);
        DirectionQuery query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .count(20)
                .next(true)
                .contentKey(new ContentKey(start.minusMinutes(1), "blah"))
                .build();
        Collection<ContentKey> found = s3ContentDao.query(query);
        assertEquals(keys.size(), found.size());
        assertTrue(keys.containsAll(found));

        query = DirectionQuery.builder()
                .stable(false)
                .channelName(channel)
                .count(3)
                .next(true)
                .contentKey(new ContentKey(start.plusMinutes(1), "blah"))
                .build();
        found = s3ContentDao.query(query);
        assertEquals(3, found.size());
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