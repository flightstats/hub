package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
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
    private static ActiveWebhooks activeWebhooks;

    @BeforeClass
    public static void setup() throws Exception {
        curator = Integration.startZooKeeper();

        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        createWebhook(EMPTY_WEBHOOK);

        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        // Reasonableness check
        List<String> webhooksBeforeInitialize = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals(4, webhooksBeforeInitialize.size());

        activeWebhooks = new ActiveWebhooks(curator);
    }

    @Test
    public void testInitialize_cleansUpEmptyLeaderNodes() throws Exception {
        List<String> webhooks = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals(3, webhooks.size());

        assertEquals(newHashSet(WEBHOOK_WITH_LOCK, WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE), newHashSet(webhooks));
    }

    @Test
    public void testGetServers_returnsSeveralDistinctServersForAWebhook() {
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_A_FEW_LEASES);
        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), servers);
    }

    @Test
    public void testGetServers_returnsSingleServerForAWebhook() {
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_LEASE);
        assertEquals(getServersWithPort(SERVER_IP1), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfThereAreNoLeases() {
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_LOCK);
        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testGetServers_returnsAnEmptyListIfTheWebhookWasEmpty() {
        Set<String> servers = activeWebhooks.getServers(EMPTY_WEBHOOK);
        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testIsActiveWebhook_isTrueIfWebhookHasLease() {
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LEASE));
    }

    @Test
    public void testIsActiveWebhook_isTrueIfWebhookHasLock() {
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LOCK));
    }

    @Test
    public void testIsActiveWebhook_isFalseIfWebhookIsEmptyAndHasBeenCleanedUp() {
        assertFalse(activeWebhooks.isActiveWebhook(EMPTY_WEBHOOK));
    }

    private static void createWebhook(String webhook) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
        curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/leases", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
    }

    private static void createWebhookLock(String webhook, String lockName, String value) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks/%s", WEBHOOK_LEADER_PATH, webhook, lockName), value.getBytes());
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
