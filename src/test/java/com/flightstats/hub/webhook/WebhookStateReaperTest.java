package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorRepository;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;
    private WebhookContentPathSet webhookInProcess;
    private WebhookErrorService webhookErrorService;
    private WebhookLeaderLocks webhookLeaderLocks;

    private static final String webhookName = "onTheHook";
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");

    @BeforeClass
    public static void setupCurator() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorRepository.ErrorNodeNameGenerator nameGenerator = new WebhookErrorRepository.ErrorNodeNameGenerator();
        WebhookErrorRepository webhookErrorRepository = new WebhookErrorRepository(zooKeeperUtils, nameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorRepository);

        webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        lastContentPath = new LastContentPath(curator);
        webhookErrorService = new WebhookErrorService(webhookErrorRepository, webhookErrorPruner, channelService);
        webhookInProcess = new WebhookContentPathSet(zooKeeperUtils);
    }

    @After
    public void teardown() throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath("/");
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookErrors() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookInProcess() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoContentWasAdded() throws Exception {
        // GIVEN
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookLeader() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
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
        webhookErrorService.add(webhook, "oops");
        assertErrorExists(webhook);
    }

    private void addWebhookLeader(String webhook) throws Exception {
        String path = WebhookLeaderLocks.WEBHOOK_LEADER + "/" + webhook + "/leases/someLease";
        curator.create().creatingParentContainersIfNeeded().forPath(path);
        curator.setData().forPath(path, "foo".getBytes());
        assertWebhookLeaderExists(webhook);
    }

    private void assertLastCompletedExists(String webhook) throws Exception {
        assertTrue(curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhook).length > 0);
    }

    private void assertWebhookInProcessExists(String webhook) {
        assertEquals(1, webhookInProcess.getSet(webhook, key).size());
    }

    private void assertErrorExists(String webhook) {
        assertEquals(1, webhookErrorService.lookup(webhook).size());
    }

    private void assertWebhookLeaderExists(String webhook) {
        assertFalse(webhookLeaderLocks.getServerLeases(webhook).isEmpty());
    }

    private void assertLastCompletedDeleted(String webhook) {
        assertThrows(KeeperException.NoNodeException.class,
                () -> curator.getData().forPath(WebhookLeader.WEBHOOK_LAST_COMPLETED + webhook));
    }

    private void assertErrorDeleted(String webhook) {
        assertTrue(webhookErrorService.lookup(webhook).isEmpty());
    }

    private void assertWebhookInProcessDeleted(String webhook) {
        assertTrue(webhookInProcess.getSet(webhook, key).isEmpty());
    }

    private void assertWebhookLeaderDeleted(String webhook) {
        assertFalse(webhookLeaderLocks.getWebhooks().contains(webhook));
    }
}
