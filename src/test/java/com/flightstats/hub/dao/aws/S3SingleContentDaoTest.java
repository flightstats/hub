package com.flightstats.hub.dao.aws;

import static org.junit.Assert.assertEquals;

public class S3SingleContentDaoTest {

//    private final static Logger logger = LoggerFactory.getLogger(S3SingleContentDaoTest.class);
//
//    private static ContentDaoTester util;
//    private static S3SingleContentDao s3SingleContentDao;
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//        HubProperties.loadProperties("useDefault");
//        Injector injector = TestApplication.startAwsHub();
//        s3SingleContentDao = injector.getInstance(S3SingleContentDao.class);
//        util = new ContentDaoTester(s3SingleContentDao);
//    }
//
//    @Test
//    public void testWriteRead() throws Exception {
//        util.testWriteRead(ContentDaoTester.createContent());
//    }
//
//    @Test
//    public void testQueryRangeDay() throws Exception {
//        util.testQueryRangeDay();
//    }
//
//    @Test
//    public void testQueryRangeHour() throws Exception {
//        util.testQueryRangeHour();
//    }
//
//    @Test
//    public void testQueryRangeMinute() throws Exception {
//        util.testQueryRangeMinute();
//    }
//
//    @Test
//    public void testQuery15Minutes() throws Exception {
//        util.testQuery15Minutes();
//    }
//
//    @Test
//    public void testDirectionQuery() throws Exception {
//        util.testDirectionQuery();
//    }
//
//    @Test
//    public void testDelete() throws Exception {
//        util.testDeleteMaxItems();
//    }
//
//    @Test
//    public void testPreviousFromBulk_Issue753() throws Exception {
//        util.testPreviousFromBulk_Issue753();
//    }
//
//    @Test
//    public void testWriteReadOld() throws Exception {
//        String channel = "testWriteReadOld";
//        Content content = ContentDaoTester.createContent();
//        ContentKey key = s3SingleContentDao.insertOld(channel, content);
//        logger.info("key {}", key);
//        assertEquals(content.getContentKey().get(), key);
//        Content read = s3SingleContentDao.get(channel, key);
//        logger.info("read {}", read.getContentKey());
//        ContentDaoTester.compare(content, read, key.toString().getBytes());
//    }
//
//    @Test
//    public void testHistorical() throws Exception {
//        util.testWriteHistorical();
//    }

}
