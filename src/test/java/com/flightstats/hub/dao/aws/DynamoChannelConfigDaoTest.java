package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public class DynamoChannelConfigDaoTest {

    private static final Logger logger = LoggerFactory.getLogger(DynamoChannelConfigDaoTest.class);
    private static DynamoChannelConfigDao channelConfigDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = TestMain.start();
        channelConfigDao = injector.getInstance(DynamoChannelConfigDao.class);
        channelConfigDao.initialize();
    }

    @Test
    public void testSimple() {
        logger.info("DynamoChannelConfigDao {}", channelConfigDao);
        assertNotNull(channelConfigDao);
        ChannelConfig channelConfig = ChannelConfig.builder().name("testsimple").build();
        channelConfigDao.upsert(channelConfig);

        ChannelConfig testSimple = channelConfigDao.get("testsimple");
        logger.info("channel {}", testSimple);
        assertNotNull(testSimple);
    }
}