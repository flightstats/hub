package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.testng.AssertJUnit.assertTrue;

public class ActiveWebhooksTest {
    private static final String WEBHOOK_LEADER_PATH = "/WebhookLeader";
    private static final int HUB_PORT = HubHost.getLocalPort();


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
        curator = Integration.startZooKeeper();
        zooKeeperUtils = new SafeZooKeeperUtils(curator);
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
    public void testCleanupEmpty_keepsWebhooksWithLeasesAndLocksAndDiscardsOthers() throws Exception {
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        createWebhook(EMPTY_WEBHOOK);

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        activeWebhooks.cleanupEmpty();

        List<String> webhooks = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals(3, webhooks.size());
        assertEquals(newHashSet(WEBHOOK_WITH_LOCK, WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE), newHashSet(webhooks));

        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_A_FEW_LEASES));
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LEASE));
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LOCK));

        assertFalse(activeWebhooks.isActiveWebhook(EMPTY_WEBHOOK));
    }

    @Test
    public void testGetServers_returnsSeveralDistinctServersForAWebhook() throws Exception {
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_A_FEW_LEASES);
        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), servers);
    }

    @Test
    public void testGetServers_returnsSingleServerForAWebhook() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_LEASE);
        assertEquals(getServersWithPort(SERVER_IP1), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfThereAreNoLeases()  throws Exception {
        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_LOCK);
        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfTheWebhookWasEmpty() throws Exception{
        createWebhook(EMPTY_WEBHOOK);

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        Set<String> servers = activeWebhooks.getServers(EMPTY_WEBHOOK);
        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListForNonExistentWebhook() throws Exception {
        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        Set<String> servers = activeWebhooks.getServers("bogusWebhook");
        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testIsActiveWebhook_isTrueIfWebhookHasLease() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LEASE));
    }

    @Test
    public void testIsActiveWebhook_isTrueIfWebhookHasLock() throws Exception {
        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LOCK));
    }

    @Test
    public void testIsActiveWebhook_isFalseIfWebhookIsEmptyAndHasBeenCleanedUp() throws Exception{
        ActiveWebhooks activeWebhooks = new ActiveWebhooks(zooKeeperUtils);
        assertFalse(activeWebhooks.isActiveWebhook("nonexistent webhook"));
    }

    private static void createWebhook(String webhook) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/leases", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static void createWebhookLock(String webhook, String lockName, String value) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks/%s", WEBHOOK_LEADER_PATH, webhook, lockName), value.getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static void createWebhookLease(String webhook, String leaseName, String value) {
        String leasePath = format("%s/%s/leases/%s", WEBHOOK_LEADER_PATH, webhook, leaseName);
        try {
            curator.create().creatingParentsIfNeeded().forPath(leasePath, value.getBytes());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:%d", server, HUB_PORT))
                .collect(toSet());
    }
}
