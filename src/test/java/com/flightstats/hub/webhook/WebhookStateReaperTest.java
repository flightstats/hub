package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;
    private WebhookContentPathSet webhookInProcess;
    private WebhookErrorService webhookErrorService;
    private WebhookLeaderLocks webhookLeaderLocks;

    private static final String webhookName = "onTheHook";
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");

    @BeforeAll
    static void setupCurator() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @BeforeEach
    void setup() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorRepository.ErrorNodeNameGenerator nameGenerator = new WebhookErrorRepository.ErrorNodeNameGenerator();
        WebhookErrorRepository webhookErrorRepository = new WebhookErrorRepository(zooKeeperUtils, nameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorRepository);

        HubProperties.setProperty(HubProperties.HubProps.WEBHOOK_LEADERSHIP_ENABLED.getKey(), "true");
        webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        lastContentPath = new LastContentPath(curator, new AppProperties(PropertiesLoader.getInstance()));
        webhookErrorService = new WebhookErrorService(webhookErrorRepository, webhookErrorPruner, channelService);
        webhookInProcess = new WebhookContentPathSet(zooKeeperUtils);
    }

    @Test
    void testCleansUpZookeeperNodesRelatedToState() throws Exception {
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
    void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookErrors() throws Exception {
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
    void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookInProcess() throws Exception {
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
    void testCleansUpZookeeperNodesRelatedToState_whenNoContentWasAdded() throws Exception {
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
    void testCleansUpZookeeperNodesRelatedToState_whenNoWebhookLeader() throws Exception {
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

    @Test
    public void testDoesNothingIfLeadershipDisabled() throws Exception {
        HubProperties.setProperty(HubProperties.HubProps.WEBHOOK_LEADERSHIP_ENABLED.getKey(), "false");
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookErrorService, webhookLeaderLocks);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
        assertWebhookLeaderExists(webhookName);
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
