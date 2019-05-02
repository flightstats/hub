package com.flightstats.hub.dao.aws;

import com.flightstats.hub.channel.ZipBulkBuilder;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class S3BatchContentDaoTest {
    
    private static S3BatchContentDao contentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        PropertiesLoader.getInstance().load("useDefault");
        PropertiesLoader.getInstance().setProperty("s3.maxQueryItems", "5");
        Injector injector = Integration.startAwsHub();
        contentDao = injector.getInstance(S3BatchContentDao.class);
    }

    @Test
    public void testBatchWriteRead() throws Exception {
        String channel = "testBatchWriteRead";
        MinutePath minutePath = new MinutePath();
        List<ContentKey> keys = writeBatchMinute(channel, minutePath, 5);

        for (ContentKey key : keys) {
            Content content = ContentDaoUtil.createContent(key);
            Content read = contentDao.get(channel, key);
            assertEquals(content.getContentKey(), read.getContentKey());
            assertArrayEquals(content.getData(), read.getData());
            assertEquals(content.getContentType().get(), read.getContentType().get());
        }

        assertCount(channel, minutePath, 0);
        keys.remove(2);
        MinutePath pathAndKeys = new MinutePath(minutePath.getTime(), keys);
        assertCount(channel, pathAndKeys, 4);
    }

    private void assertCount(String channel, MinutePath pathAndKeys, int expected) {
        AtomicInteger count = new AtomicInteger();
        contentDao.streamMinute(channel, pathAndKeys, false, content -> {
                    ContentKey key = content.getContentKey().get();
                    log.info("found content {}", key);
                    count.incrementAndGet();
                    assertTrue(pathAndKeys.getKeys().contains(key));
                }
        );
        assertEquals(expected, count.get());
    }

    private List<ContentKey> writeBatchMinute(String channel, MinutePath minutePath, int count) throws IOException {
        List<ContentKey> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ContentKey contentKey = new ContentKey(minutePath.getTime().plusSeconds(i), "" + i);
            keys.add(contentKey);
            log.info("adding {}", contentKey);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream output = new ZipOutputStream(baos);
        for (ContentKey key : keys) {
            Content content = ContentDaoUtil.createContent(key);
            ZipBulkBuilder.createZipEntry(output, content);
        }
        output.close();

        byte[] bytes = baos.toByteArray();
        contentDao.writeBatch(channel, minutePath, keys, bytes);
        return keys;
    }

    @Test
    public void testQueryMinute() throws IOException {
        String channel = "testQueryMinute" + StringUtils.randomAlphaNumeric(20);
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
        String channel = "testQueryHour" + StringUtils.randomAlphaNumeric(20);
        DateTime start = TimeUtil.now().withMinuteOfHour(54);
        ContentKey key = new ContentKey(start, "start");
        for (int i = 0; i < 12; i++) {
            writeBatchMinute(channel, new MinutePath(start.plusMinutes(i * 6)), 2);
        }
        query(channel, start, 2, TimeUtil.Unit.HOURS);
        query(channel, start.plusMinutes(6), 20, TimeUtil.Unit.HOURS);
        query(channel, start.plusMinutes(66), 2, TimeUtil.Unit.HOURS);
    }

    private void query(String channel, DateTime start, int expected, TimeUtil.Unit unit) {
        ActiveTraces.start("S3BatchContentDaoTest", channel, start, expected, unit);
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(start)
                .unit(unit)
                .build();
        SortedSet<ContentKey> found = contentDao.queryByTime(timeQuery);
        log.info("found {}", found);
        ActiveTraces.getLocal().log(log);
        assertEquals(expected, found.size());
        ActiveTraces.end();
    }

    @Test
    public void testMissing() {
        String channel = "testMissing";
        Content content = contentDao.get(channel, new ContentKey());
        assertNull(content);
        TimeQuery timeQuery = TimeQuery.builder().channelName(channel)
                .startTime(new MinutePath().getTime())
                .unit(TimeUtil.Unit.MINUTES)

                .build();
        SortedSet<ContentKey> found = contentDao.queryByTime(timeQuery);
        log.info("minute {}", found);

        assertEquals(0, found.size());
    }

    @Test
    public void testDirectionQueryAndDelete() throws Exception {
        String channel = "testDirectionQuery" + StringUtils.randomAlphaNumeric(20);
        DateTime start = TimeUtil.now().minusHours(2);
        ContentKey key = new ContentKey(new MinutePath(start).getTime(), "-");
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

    @Test
    public void testPreviousEdgeCase() throws Exception {
        String channel = "testPreviousEdgeCase" + StringUtils.randomAlphaNumeric(20);
        DateTime start = TimeUtil.now().minusHours(2);
        List<ContentKey> contentKeys = writeBatchMinute(channel, new MinutePath(start), 4);
        log.info("wrote keys ", contentKeys);
        SortedSet<ContentKey> foundKeys = queryDirection(channel, contentKeys.get(3), false, 2, 2);
        log.info("query found {}", foundKeys);
        assertTrue(foundKeys.contains(contentKeys.get(1)));
        assertTrue(foundKeys.contains(contentKeys.get(2)));
    }

    private SortedSet<ContentKey> queryDirection(String channel, ContentKey contentKey, boolean next, int count, int expected) {

        DirectionQuery query =
                DirectionQuery.builder()
                        .channelName(channel)
                        .channelConfig(ChannelConfig.builder().name(channel).build())
                        .startKey(contentKey)
                        .next(next)
                        .count(count)
                        .earliestTime(TimeUtil.now().minusDays(2))
                        .build();

        ActiveTraces.start(query);
        log.info("running query {}", query);
        SortedSet<ContentKey> found = contentDao.query(query);
        ActiveTraces.getLocal().log(log);
        ActiveTraces.end();
        assertEquals(expected, found.size());
        return found;
    }

    @Test
    public void testDirectionQueryBug() throws Exception {
        String channel = "testDirectionQueryBug" + StringUtils.randomAlphaNumeric(20);
        DateTime start = TimeUtil.now().minusHours(2);
        MinutePath startItem = new MinutePath(start);
        writeBatchMinute(channel, startItem, 20);
        writeBatchMinute(channel, new MinutePath(start.plusMinutes(1)), 20);

        ContentKey key = new ContentKey(startItem.getTime(), "-1");
        queryDirection(channel, key, true, 50, 40);
        queryDirection(channel, new ContentKey(key.getTime().plusSeconds(18), "-2"), true, 10, 10);
        queryDirection(channel, new ContentKey(key.getTime().plusSeconds(18), "-2"), true, 30, 22);
    }

}