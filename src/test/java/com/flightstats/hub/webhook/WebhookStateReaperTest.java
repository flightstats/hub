package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private ChannelService channelService = mock(ChannelService.class);

    @BeforeClass
    public static void setupCurator() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState() throws Exception {
        LastContentPath lastContentPath = new LastContentPath(curator);
        WebhookContentPathSet webhookInProcess = new WebhookContentPathSet(curator);
        WebhookError webhookError = new WebhookError(curator, channelService);

        String webhookName = "onTheHook";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        lastContentPath.initialize(webhookName, key1, WebhookLeader.WEBHOOK_LAST_COMPLETED);

        webhookInProcess.add(webhookName, key1);

        webhookError.add(webhookName, "oops");

        // make sure the givens did what we asked...
        assertTrue(curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhookName).length > 0);
        assertEquals(1, webhookError.get(webhookName).size());
        assertEquals(1, webhookInProcess.getSet(webhookName, key1).size());

        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError);
        reaper.delete(webhookName);

        assertThrows(KeeperException.NoNodeException.class,
                () -> curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhookName));
        assertTrue(webhookError.get(webhookName).isEmpty());
        assertTrue(webhookInProcess.getSet(webhookName, key1).isEmpty());
    }
}
