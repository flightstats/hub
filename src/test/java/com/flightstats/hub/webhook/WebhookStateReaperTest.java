package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;
    private WebhookContentPathSet webhookInProcess;
    private WebhookError webhookError;

    private static final String webhookName = "onTheHook";
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");

    @BeforeClass
    public static void setupCurator() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        ChannelService channelService = mock(ChannelService.class);
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorService webhookErrorService = new WebhookErrorService(zooKeeperUtils);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorService);

        lastContentPath = new LastContentPath(curator);
        webhookError = new WebhookError(webhookErrorService, webhookErrorPruner, channelService);
        webhookInProcess = new WebhookContentPathSet(zooKeeperUtils);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookErrors() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookInProcess() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoContentWasAdded() throws Exception {
        // GIVEN
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
    }

    private void addLastCompleted(String webhook) throws Exception {
        lastContentPath.initialize(webhook, key, WebhookLeader.WEBHOOK_LAST_COMPLETED);
        assertLastCompletedExists(webhook);
    }

    private void addWebhookInProcess(String webhook) {
        webhookInProcess.add(webhook, key);
        assertWebhookInProcessExists(webhook);
    }

    private void addError(String webhook) {
        webhookError.add(webhook, "oops");
        assertErrorExists(webhook);
    }

    private void assertLastCompletedExists(String webhook) throws Exception {
        assertTrue(curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhook).length > 0);
    }

    private void assertWebhookInProcessExists(String webhook) {
        assertEquals(1, webhookInProcess.getSet(webhook, key).size());
    }

    private void assertErrorExists(String webhook) {
        assertEquals(1, webhookError.get(webhook).size());
    }

    private void assertLastCompletedDeleted(String webhook) {
        assertThrows(KeeperException.NoNodeException.class,
                () -> curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhook));
    }

    private void assertErrorDeleted(String webhook) {
        assertTrue(webhookError.get(webhook).isEmpty());
    }

    private void assertWebhookInProcessDeleted(String webhook) {
        assertTrue(webhookInProcess.getSet(webhook, key).isEmpty());
    }
}
