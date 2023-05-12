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
import org.junit.jupiter.api.AfterEach;
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
class RunningStateReaperTest {
    private static final DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
    private static final ContentKey key = new ContentKey(start, "B");
    private static final String webhookName = "onTheHook";

    private CuratorFramework curator;
    private ClusterCacheDao clusterCacheDao;
    private WebhookContentInFlight contentKeysInFlight;
    private WebhookErrorService webhookErrorService;
    private WebhookLeaderLocks webhookLeaderLocks;
    @Mock
    private WebhookProperties webhookProperties;
    @Mock
    private AppProperties appProperties;

    @BeforeEach
    void setup() throws Exception {
        curator = IntegrationTestSetup.run().getZookeeperClient();
        ChannelService channelService = mock(ChannelService.class);
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorRepository.ErrorNodeNameGenerator nameGenerator = new WebhookErrorRepository.ErrorNodeNameGenerator();
        WebhookErrorRepository webhookErrorRepository = new WebhookErrorRepository(zooKeeperUtils, nameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorRepository);

        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        clusterCacheDao = new ClusterCacheDao(curator, appProperties);
        webhookErrorService = new WebhookErrorService(webhookErrorRepository, webhookErrorPruner, channelService);
        contentKeysInFlight = new WebhookContentInFlight(zooKeeperUtils);
    }

    @AfterEach
    void teardown() throws Exception {
        // ...AND then clean up the state so the next test can run.  Eww. Ideally this would manually clear ZK nodes, I think.
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);
    }

    @Test
    void testDelete_cleansUpZookeeperNodesRelatedToState() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDelete_cleansUpZookeeperNodesRelatedToState_whenNoWebhookErrors() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDelete_cleansUpZookeeperNodesRelatedToState_whenNoWebhookInProcess() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDelete_cleansUpZookeeperNodesRelatedToState_whenNoContentWasAdded() throws Exception {
        // GIVEN
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDelete_cleansUpZookeeperNodesRelatedToState_whenNoWebhookLeader() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
        assertWebhookLeaderDeleted(webhookName);
    }

    @Test
    void testDelete_doesNothing_whenLeadershipDisabled() throws Exception {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
        assertWebhookLeaderExists(webhookName);
    }

    @Test
    void testStop_cleansUpOnlyZookeeperLeadershipState() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertWebhookLeaderDeleted(webhookName);

        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
    }

    @Test
    void testStop_cleansUpZookeeperWebhookLeaderState_whenNoWebhookErrors() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertWebhookLeaderDeleted(webhookName);
        assertErrorDeleted(webhookName);

        assertLastCompletedExists(webhookName);
        assertWebhookInProcessExists(webhookName);
    }

    @Test
    void testStop_cleansUpZookeeperWebhookLeaderState_whenNoWebhookInProcess() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertWebhookLeaderDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);

        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
    }

    @Test
    void testStop_cleansUpZookeeperWebhookLeaderState_whenNoContentWasAdded() throws Exception {
        // GIVEN
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertWebhookLeaderDeleted(webhookName);
        assertLastCompletedDeleted(webhookName);

        assertWebhookInProcessExists(webhookName);
        assertErrorExists(webhookName);
    }

    @Test
    void testStop_cleansUpNothing_whenNoWebhookLeader() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertWebhookLeaderDeleted(webhookName);
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
    }

    @Test
    void testStop_doesNothing_whenLeadershipDisabled() throws Exception {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);
        addWebhookLeader(webhookName);

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(clusterCacheDao, contentKeysInFlight, webhookErrorService, webhookLeaderLocks, webhookProperties);
        reaper.stop(webhookName);

        // THEN
        assertLastCompletedExists(webhookName);
        assertErrorExists(webhookName);
        assertWebhookInProcessExists(webhookName);
        assertWebhookLeaderExists(webhookName);
    }

    private void addLastCompleted(String webhook) throws Exception {
        clusterCacheDao.initialize(webhook, key, WEBHOOK_LAST_COMPLETED);
        assertLastCompletedExists(webhook);
    }

    private void addWebhookInProcess(String webhook) {
        contentKeysInFlight.add(webhook, key);
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
        assertEquals(1, contentKeysInFlight.getSet(webhook, key).size());
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
        assertTrue(contentKeysInFlight.getSet(webhook, key).isEmpty());
    }

    private void assertWebhookLeaderDeleted(String webhook) {
        assertFalse(webhookLeaderLocks.getWebhooks().contains(webhook));
    }
}
