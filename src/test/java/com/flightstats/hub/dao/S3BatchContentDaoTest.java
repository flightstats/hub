package com.flightstats.hub.dao;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ZipBatchBuilder;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.S3BatchContentDao;
import com.flightstats.hub.dao.aws.S3BucketName;
import com.flightstats.hub.metrics.NoOpMetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class S3BatchContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchContentDaoTest.class);
    private static S3BatchContentDao contentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        HubProperties.setProperty("s3.maxQueryItems", "5");
        contentDao = new S3BatchContentDao(s3Client, bucketName, new NoOpMetricsSender());
    }

    @Test
    public void testBatchWriteRead() throws Exception {
        String channel = "testBatchWriteRead";
        List<ContentKey> keys = writeBatchMinute(channel, new MinutePath(), 5);

        for (ContentKey key : keys) {
            Content content = ContentDaoUtil.createContent(key);
            Content read = contentDao.read(channel, key);
            assertEquals(content.getContentKey(), read.getContentKey());
            assertArrayEquals(content.getData(), read.getData());
            assertEquals(content.getContentLanguage().get(), read.getContentLanguage().get());
            assertEquals(content.getContentType().get(), read.getContentType().get());
        }
    }

    private List<ContentKey> writeBatchMinute(String channel, MinutePath minutePath, int count) throws IOException {
        List<ContentKey> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ContentKey contentKey = new ContentKey(minutePath.getTime().plusSeconds(i), "" + i);
            keys.add(contentKey);
            logger.info("adding {}", contentKey);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream output = new ZipOutputStream(baos);
        for (ContentKey key : keys) {
            Content content = ContentDaoUtil.createContent(key);
            ZipBatchBuilder.createZipEntry(output, key, content);
        }
        output.close();

        byte[] bytes = baos.toByteArray();
        contentDao.writeBatch(channel, minutePath, keys, bytes);
        return keys;
    }

    @Test
    public void testQueryMinute() throws IOException {
        String channel = "testQueryMinute" + RandomStringUtils.randomAlphanumeric(20);
        DateTime start = TimeUtil.now().minusMinutes(10);
        ContentKey key = new ContentKey(start, "start");
        for (int i = 0; i < 5; i++) {
            writeBatchMinute(channel, new MinutePath(start.plusMinutes(i)), 2);
        }
        query(channel, start, 2, TimeUtil.Unit.MINUTES);
        query(channel, start.plusMinutes(2), 2, TimeUtil.Unit.MINUTES);

        DateTime secondsTime = new MinutePath(start.plusMinutes(1)).getTime();
        query(channel, secondsTime, 1, TimeUtil.Unit.SECONDS);
        query(channel, secondsTime.plusSeconds(1), 1, TimeUtil.Unit.SECONDS);
        query(channel, secondsTime.plusSeconds(2), 0, TimeUtil.Unit.SECONDS);
    }

    @Test
    public void testQueryHour() throws IOException {
        String channel = "testQueryHour" + RandomStringUtils.randomAlphanumeric(20);
        DateTime start = TimeUtil.now().withMinuteOfHour(59);
        ContentKey key = new ContentKey(start, "start");
        for (int i = 0; i < 12; i++) {
            writeBatchMinute(channel, new MinutePath(start.plusMinutes(i * 6)), 2);
        }
        query(channel, start, 2, TimeUtil.Unit.HOURS);
        query(channel, start.plusMinutes(2), 20, TimeUtil.Unit.HOURS);
        query(channel, start.plusMinutes(61), 2, TimeUtil.Unit.HOURS);
    }

    private void query(String channel, DateTime start, int expected, TimeUtil.Unit unit) {
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(unit)
                .build();
        SortedSet<ContentKey> found = contentDao.queryByTime(timeQuery);
        assertEquals(expected, found.size());
    }

    @Test
    public void testMissing() {
        String channel = "testMissing";
        Content content = contentDao.read(channel, new ContentKey());
        assertNull(content);
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(new MinutePath().getTime())
                .unit(TimeUtil.Unit.MINUTES)

                .build();
        SortedSet<ContentKey> found = contentDao.queryByTime(timeQuery);
        logger.info("minute {}", found);

        assertEquals(0, found.size());
    }

    @Test
    public void testDirectionQueryAndDelete() throws Exception {
        String channel = "testDirectionQuery" + RandomStringUtils.randomAlphanumeric(20);
        DateTime start = TimeUtil.now().minusHours(2);
        ContentKey key = new ContentKey(start, "start");
        for (int i = 0; i < 12; i++) {
            writeBatchMinute(channel, new MinutePath(start.plusMinutes(i * 6)), 2);
        }

        queryDirection(channel, key, true, 50, 24);
        queryDirection(channel, new ContentKey(start.plusMinutes(37), "A"), true, 6, 6);
        queryDirection(channel, new ContentKey(start.plusMinutes(73), "A"), true, 0, 0);

        queryDirection(channel, new ContentKey(start.plusMinutes(73), "A"), false, 23, 23);
        queryDirection(channel, new ContentKey(start.plusMinutes(14), "A"), false, 8, 6);

        contentDao.deleteBefore(channel, new ContentKey(start.plusMinutes(37), "A"));
        queryDirection(channel, new ContentKey(start.plusMinutes(37), "A"), false, 8, 0);
        queryDirection(channel, key, true, 50, 10);

    }

    private void queryDirection(String channel, ContentKey contentKey, boolean next, int count, int expected) {
        TracesImpl traces = new TracesImpl();
        DirectionQuery query =
                DirectionQuery.builder()
                        .channelName(channel)
                        .contentKey(contentKey)
                        .next(next)
                        .count(count)
                        .ttlDays(2)
                        .traces(traces)
                        .build();

        SortedSet<ContentKey> found = contentDao.query(query);
        traces.log(logger);
        assertEquals(expected, found.size());
    }

}