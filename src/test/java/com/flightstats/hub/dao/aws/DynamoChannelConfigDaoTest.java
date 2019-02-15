package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DynamoChannelConfigDaoTest {

    private static final Logger logger = LoggerFactory.getLogger(DynamoChannelConfigDaoTest.class);
    private static DynamoChannelConfigDao channelConfigDao;

    @BeforeAll
    public static void setUpClass() throws Exception {
        logger.info("setting up ...");
        Injector injector = Integration.startAwsHub();
        channelConfigDao = HubProvider.getInstance(DynamoChannelConfigDao.class);
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