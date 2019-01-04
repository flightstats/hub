package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WebhookLeaderLocksTest {
    private static final String WEBHOOK_LEADER_PATH = "/WebhookLeader";

    private static final String WEBHOOK_WITH_LEASE = "webhook1";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "webhook4";
    private static final String WEBHOOK_WITH_LOCK = "webhook3";
    private static final String EMPTY_WEBHOOK = "webhook2";

    private static final String SERVER_IP1 = "10.2.1";
    private static final String SERVER_IP2 = "10.2.2";

    private static CuratorFramework curator;
    private static SafeZooKeeperUtils zooKeeperUtils;

    @BeforeClass
    public static void setup() throws Exception {
        ZooKeeperState zooKeeperState = new ZooKeeperState();
        curator = Integration.startZooKeeper(zooKeeperState);
        zooKeeperUtils = new SafeZooKeeperUtils(curator, zooKeeperState);
    }

    @Before
    public void createWebhookLeader() throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(WEBHOOK_LEADER_PATH);
    }

    @After
    public void destroyWebhookLeaders() throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath(WEBHOOK_LEADER_PATH);
    }

    @Test
    public void testDeleteWebhook() throws Exception {
        createWebhook(EMPTY_WEBHOOK);

        createWebhookLock(WEBHOOK_WITH_A_FEW_LEASES, "someLock", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        List<String> initialWebhooks = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals("givens should have created 2 webhooks", 2, initialWebhooks.size());

        WebhookLeaderLocks webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        webhookLeaderLocks.deleteWebhookLeader(WEBHOOK_WITH_A_FEW_LEASES);

        List<String> webhooks = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals(1, webhooks.size());
        assertEquals(newHashSet(EMPTY_WEBHOOK), newHashSet(webhooks));
    }

    @Test
    public void testGetServers_returnsSeveralDistinctServersForAWebhook() throws Exception {
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        WebhookLeaderLocks webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> servers = webhookLeaderLocks.getServerLeases(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(newHashSet(SERVER_IP1, SERVER_IP2), servers);
    }

    @Test
    public void testGetServers_returnsSingleServerForAWebhook() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> servers = webhookLeaders.getServerLeases(WEBHOOK_WITH_LEASE);

        assertEquals(newHashSet(SERVER_IP1), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfThereAreNoLeases()  throws Exception {
        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> servers = webhookLeaders.getServerLeases(WEBHOOK_WITH_LOCK);

        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfTheWebhookWasEmpty() throws Exception{
        createWebhook(EMPTY_WEBHOOK);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> servers = webhookLeaders.getServerLeases(EMPTY_WEBHOOK);

        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListForNonExistentWebhook() throws Exception {
        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> servers = webhookLeaders.getServerLeases("bogusWebhook");

        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetWebhooks_returnsAllWebhooksRegardlessOfLocksOrLeases() throws Exception {
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        createWebhook(EMPTY_WEBHOOK);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        Set<String> webhooks = webhookLeaders.getWebhooks();
        assertEquals(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK, EMPTY_WEBHOOK), webhooks);
    }

    @Test
    public void testGetLocks() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LOCK, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LOCK, "someLease", SERVER_IP1);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> locks = webhookLeaders.getLockPaths(WEBHOOK_WITH_LOCK);

        assertEquals(newArrayList("someLock"), locks);
    }

    @Test
    public void testGetLocks_withNoLocks() throws Exception {
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> locks = webhookLeaders.getLockPaths(WEBHOOK_WITH_LEASE);

        assertEquals(emptyList(), locks);
    }

    @Test
    public void testGetLocks_noWebhook() throws Exception {
        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> locks = webhookLeaders.getLockPaths("dunno");

        assertEquals(emptyList(), locks);
    }

    @Test
    public void testGetLeases() throws Exception {
        createWebhookLock(WEBHOOK_WITH_A_FEW_LEASES, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP1);

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> leases = webhookLeaders.getLeasePaths(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(3, leases.size());
        assertEquals(newHashSet("someLease", "someLease2", "someLease3"), newHashSet(leases));
    }

    @Test
    public void testGetLeases_withNoLeases() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LOCK, "someLock", "");

        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> leases = webhookLeaders.getLeasePaths(WEBHOOK_WITH_LOCK);

        assertEquals(emptyList(), leases);
    }

    @Test
    public void testGetLeases_noWebhook() throws Exception {
        WebhookLeaderLocks webhookLeaders = new WebhookLeaderLocks(zooKeeperUtils);
        List<String> leases = webhookLeaders.getLeasePaths("dunno");

        assertEquals(emptyList(), leases);
    }

    private static void createWebhook(String webhook) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(getLeasePath(webhook), "".getBytes());
            curator.create().creatingParentsIfNeeded().forPath(getLockPath(webhook), "".getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static void createWebhookLock(String webhook, String lockName, String value) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(getLockPath(webhook, lockName), value.getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static void createWebhookLease(String webhook, String leaseName, String value) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(getLeasePath(webhook, leaseName), value.getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static String getLockPath(String webhook) {
        return format("%s/%s/locks", WEBHOOK_LEADER_PATH, webhook);
    }

    private static String getLockPath(String webhook, String lockName) {
        return format("%s/%s", getLockPath(webhook), lockName);
    }

    private static String getLeasePath(String webhook) {
        return format("%s/%s/leases", WEBHOOK_LEADER_PATH, webhook);
    }

    private static String getLeasePath(String webhook, String leaseName) {
        return format("%s/%s", getLeasePath(webhook), leaseName);
    }
}
