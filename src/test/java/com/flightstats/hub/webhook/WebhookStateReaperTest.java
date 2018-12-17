package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebhookStateReaperTest {
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;
    private WebhookContentPathSet webhookInProcess;
    private WebhookError webhookError;
    private Dao<Webhook> webhookDao;

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
        webhookDao = (Dao<Webhook>) mock(Dao.class);

        lastContentPath = new LastContentPath(curator);
        webhookError = new WebhookError(curator, channelService);
        webhookInProcess = new WebhookContentPathSet(curator);
    }

    @Test
    public void testCleansUpZookeeperNodesRelatedToState() throws Exception {
        // GIVEN
        addLastCompleted(webhookName);
        addWebhookInProcess(webhookName);
        addError(webhookName);

        // make sure the givens did what we asked...

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError, webhookDao);
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
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError, webhookDao);
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
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError, webhookDao);
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
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError, webhookDao);
        reaper.delete(webhookName);

        // THEN
        assertLastCompletedDeleted(webhookName);
        assertErrorDeleted(webhookName);
        assertWebhookInProcessDeleted(webhookName);
    }

    @Test
    public void testReapDeleted_removesOnlyWebhooksThatDoNotExist() throws Exception {
        // GIVEN
        when(webhookDao.getAll(false)).thenReturn(newArrayList(
                Webhook.builder().name("1").build(),
                Webhook.builder().name("2").build(),
                Webhook.builder().name("3").build(),
                Webhook.builder().name("4").build(),
                Webhook.builder().name("z").build()
        ));

        addLastCompleted("1");
        addLastCompleted("2");
        addLastCompleted("5");

        addError("1");
        addError("3");
        addError("6");

        addWebhookInProcess("1");
        addWebhookInProcess("4");
        addWebhookInProcess("7");

        // WHEN
        WebhookStateReaper reaper = new WebhookStateReaper(lastContentPath, webhookInProcess, webhookError, webhookDao);
        reaper.reapStateForDeletedWebhooks();

        // THEN
        assertLastCompletedExists("1");
        assertErrorExists("1");
        assertWebhookInProcessExists("1");

        assertLastCompletedExists("2");

        assertErrorExists("3");

        assertWebhookInProcessExists("4");

        assertLastCompletedDeleted("5");

        assertErrorDeleted("6");

        assertWebhookInProcessDeleted("7");
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
