package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.WebhookProperties;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness= Strictness.LENIENT)
class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private ClusterStateDao clusterStateDao;
    private WebhookContentPathSet webhookInProcess;
    private WebhookErrorService webhookErrorService;
    private WebhookLeaderLocks webhookLeaderLocks;
    @Mock
    private WebhookProperties webhookProperties;

    private static final String webhookName = "onTheHook";
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");

    @BeforeAll
    static void runFirst() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @BeforeEach
    void setup() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorRepository.ErrorNodeNameGenerator nameGenerator = new WebhookErrorRepository.ErrorNodeNameGenerator();
        WebhookErrorRepository webhookErrorRepository = new WebhookErrorRepository(zooKeeperUtils, nameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorRepository);

        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        clusterStateDao = new ClusterStateDao(curator, new AppProperties(PropertiesLoader.getInstance()));
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    public void testDoesNothingIfLeadershipDisabled() throws Exception {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
        assertWebhookLeaderExists(webhookName);

        // ...AND then clean up the state so the next test can run.  Eww.
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        reaper = new WebhookStateReaper(clusterStateDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);
    }

    private void addLastCompleted(String webhook) throws Exception {
        clusterStateDao.initialize(webhook, key, WebhookLeader.WEBHOOK_LAST_COMPLETED);
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
