package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class DynamoChannelConfigDaoTest {

    private static DynamoChannelConfigDao channelConfigDao;

    @BeforeAll
    static void setUpClass() throws Exception {
        log.info("setting up ...");
        Injector injector = Integration.startAwsHub();
        channelConfigDao = injector.getInstance(DynamoChannelConfigDao.class);
        channelConfigDao.initialize();
    }

    @Test
    void testSimple() {
        log.info("DynamoChannelConfigDao {}", channelConfigDao);
        assertNotNull(channelConfigDao);
        ChannelConfig channelConfig = ChannelConfig.builder().name("testsimple").build();
        channelConfigDao.upsert(channelConfig);

        ChannelConfig testSimple = channelConfigDao.get("testsimple");
        log.info("channel {}", testSimple);
        assertNotNull(testSimple);
    }
}