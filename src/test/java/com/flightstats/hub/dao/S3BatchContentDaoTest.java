package com.flightstats.hub.dao;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ZipBatchBuilder;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.S3BucketName;
import com.flightstats.hub.metrics.NoOpMetricsSender;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class S3BatchContentDaoTest {

    private static S3BatchContentDao contentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        contentDao = new S3BatchContentDao(s3Client, bucketName, new NoOpMetricsSender());
    }

    @Test
    public void testBatchWriteRead() throws Exception {
        String channel = "testBatchWriteRead";
        List<ContentKey> keys = new ArrayList<>();
        MinutePath minutePath = new MinutePath();
        for (int i = 0; i < 10; i++) {
            ContentKey contentKey = new ContentKey(minutePath.getTime().plusSeconds(i), "" + i);
            keys.add(contentKey);
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

        for (ContentKey key : keys) {
            Content content = ContentDaoUtil.createContent(key);
            Content read = contentDao.read(channel, key);
            assertEquals(content.getContentKey(), read.getContentKey());
            assertArrayEquals(content.getData(), read.getData());
        }
    }

    /*
    //todo - gfm - 10/22/15 -

    @Test
    public void testQueryRangeDay() throws Exception {
        util.testQueryRangeDay();
    }

    @Test
    public void testQueryRangeHour() throws Exception {
        util.testQueryRangeHour();
    }

    @Test
    public void testQueryRangeMinute() throws Exception {
        util.testQueryRangeMinute();
    }

    @Test
    public void testDirectionQuery() throws Exception {
        util.testDirectionQuery();
    }

    @Test
    public void testDelete() throws Exception {
        util.testDeleteMaxItems();
    }*/
}