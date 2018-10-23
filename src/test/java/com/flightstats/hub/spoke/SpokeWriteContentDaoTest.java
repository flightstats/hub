package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.dao.ContentDaoTester;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.TestMain;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpokeWriteContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(SpokeWriteContentDaoTest.class);

    private ContentDaoTester daoTester;
    private HubProperties hubProperties;

    @BeforeClass
    public void setUpClass() throws Exception {
        Injector injector = TestMain.start();
        daoTester = new ContentDaoTester(injector.getInstance(SpokeWriteContentDao.class));
        hubProperties = injector.getInstance(HubProperties.class);
        CuratorCluster curatorCluster = injector.getInstance(Key.get(CuratorCluster.class, Names.named("HubCluster")));

        for (int i = 0; i < 10; i++) {
            if (curatorCluster.getAllServers().size() == 0) {
                logger.info("no servers yet...");
                Sleeper.sleep(500);
            } else {
                logger.info("servers {}", curatorCluster.getAllServers());
                return;
            }
        }

        logger.info("no servers found");
    }

    @Test
    public void testInsert() throws Exception {
        SpokeContentDao spokeContentDao = mock(SpokeContentDao.class);
        RemoteSpokeStore remoteSpokeStore = mock(RemoteSpokeStore.class);
        HubProperties hubProperties = mock(HubProperties.class);
        SpokeWriteContentDao spokeWriteContentDao = new SpokeWriteContentDao(spokeContentDao, remoteSpokeStore, hubProperties);

        String channelName = "testWriteRead";
        ContentKey contentKey = new ContentKey();
        Content content = Content.builder()
                .withContentKey(contentKey)
                .withContentType("stuff")
                .withData(contentKey.toString().getBytes())
                .build();
        String remoteSpokeStorePath = spokeWriteContentDao.getPath(channelName, contentKey);

        when(remoteSpokeStore.insert(SpokeStore.WRITE, remoteSpokeStorePath, content.getData(), "payload", channelName)).thenReturn(true);
        ContentKey returnedContentKey = spokeWriteContentDao.insert(channelName, content);

        assertEquals(contentKey, returnedContentKey);
        verify(remoteSpokeStore, times(1)).insert(SpokeStore.WRITE, remoteSpokeStorePath, content.getData(), "payload", channelName);
    }

    @Test
    public void testWriteRead() throws Exception {
        Content content = ContentDaoTester.createContent();
        daoTester.testWriteRead(content);
    }

    @Test
    public void testQueryRangeDay() throws Exception {
        daoTester.testQueryRangeDay();
    }

    @Test
    public void testQueryRangeHour() throws Exception {
        daoTester.testQueryRangeHour();
    }

    @Test
    public void testQueryRangeMinute() throws Exception {
        daoTester.testQueryRangeMinute();
    }

    @Test
    public void testQuery15Minutes() throws Exception {
        daoTester.testQuery15Minutes();
    }

    @Test
    public void testDirectionQuery() throws Exception {
        hubProperties.setProperty("spoke.write.ttlMinutes", "240");
        daoTester.testDirectionQueryTTL();
    }

    @Test
    public void testEarliest() throws Exception {
        daoTester.testEarliest();
    }

    @Test
    public void testBulkWrite() throws Exception {
        daoTester.testBulkWrite();
    }

    @Test
    public void testEmptyQuery() throws Exception {
        daoTester.testEmptyQuery();

    }

    @Test
    public void testEmptyQueryReplicated() throws Exception {
        daoTester.testEmptyQuery();

    }

    @Test
    public void testPreviousFromBulk_Issue753() throws Exception {
        daoTester.testPreviousFromBulk_Issue753();
    }
}
