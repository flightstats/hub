package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.IntegrationTestSetup;
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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;
import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebhookStateReaperTest {
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");
    private static final String webhookName = "onTheHook";

    private CuratorFramework curator;
    private ClusterCacheDao clusterCacheDao;
    private WebhookContentPathSet webhookInProcess;
    private WebhookErrorService webhookErrorService;
    private WebhookLeaderLocks webhookLeaderLocks;
    @Mock
    private WebhookProperties webhookProperties;
    @Mock
    private AppProperties appProperties;

    @BeforeAll
    void runFirst() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
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
        clusterCacheDao = new ClusterCacheDao(curator, appProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
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
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDoesNothingIfLeadershipDisabled() throws Exception {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
        assertWebhookLeaderExists(webhookName);

        // ...AND then clean up the state so the next test can run.  Eww.
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        reaper = new WebhookStateReaper(clusterCacheDao, webhookInProcess, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);
    }

    private void addLastCompleted(String webhook) throws Exception {
        clusterCacheDao.initialize(webhook, key, WEBHOOK_LAST_COMPLETED);
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
        String path = WEBHOOK_LEADER + "/" + webhook + "/leases/someLease";
        curator.create().creatingParentContainersIfNeeded().forPath(path);
        curator.setData().forPath(path, "foo".getBytes());
        assertWebhookLeaderExists(webhook);
    }

    private void assertLastCompletedExists(String webhook) throws Exception {
        assertTrue(curator.getData().forPath(WEBHOOK_LAST_COMPLETED + webhook).length > 0);
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
                () -> curator.getData().forPath(WEBHOOK_LAST_COMPLETED + webhook));
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
