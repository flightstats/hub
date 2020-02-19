package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.zookeeper.KeeperException.NodeExistsException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockitoExtension.class)
class ActiveWebhooksIntTest {
    private static final String WEBHOOK_LEADER_PATH = "/WebhookLeader";
    private static final String WEBHOOK_WITH_LEASE = "webhook1";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "webhook4";
    private static final String WEBHOOK_WITH_LOCK = "webhook3";
    private static final String EMPTY_WEBHOOK = "webhook2";

    private static final String SERVER_IP1 = "10.2.1";
    private static final String SERVER_IP2 = "10.2.2";

    private static CuratorFramework curator;
    private static SafeZooKeeperUtils zooKeeperUtils;
    @Mock
    private WebhookProperties webhookProperties;
    @Mock
    private LocalHostProperties localHostProperties;

    void createPath() throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(WEBHOOK_LEADER_PATH);
    }

    void deletePath() throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath(WEBHOOK_LEADER_PATH);
    }

    @BeforeAll
    static void setup() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
        zooKeeperUtils = new SafeZooKeeperUtils(curator);
    }

    @BeforeEach
    void createWebhookLeader() throws Exception {
        try {
            createPath();
        } catch (NodeExistsException e) {
            log.error(e.getMessage());
            deletePath();
            createPath();
        }
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
    }

    @AfterEach
    void destroyWebhookLeaders() throws Exception {
        deletePath();
    }

    @Test
    void testCleanupEmpty_keepsWebhooksWithLeasesAndLocksAndDiscardsOthers() throws Exception {
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease", SERVER_IP1);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease2", SERVER_IP2);
        createWebhookLease(WEBHOOK_WITH_A_FEW_LEASES, "someLease3", SERVER_IP2);

        createWebhookLock(WEBHOOK_WITH_LEASE, "someLock", "");
        createWebhookLease(WEBHOOK_WITH_LEASE, "someLease", SERVER_IP1);

        createWebhookLock(WEBHOOK_WITH_LOCK, "aLock", "???");

        createWebhook(EMPTY_WEBHOOK);

        WebhookLeaderLocks webhookLeaderLocks = new WebhookLeaderLocks(zooKeeperUtils);
        ActiveWebhookSweeper activeWebhookSweeper = new ActiveWebhookSweeper(webhookLeaderLocks, mock(StatsdReporter.class));
        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties, localHostProperties);

        List<String> webhooks = curator.getChildren().forPath(WEBHOOK_LEADER_PATH);
        assertEquals(3, webhooks.size());
        assertEquals(newHashSet(WEBHOOK_WITH_LOCK, WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE), newHashSet(webhooks));

        ActiveWebhooks.WebhookState state = ActiveWebhooks.WebhookState.builder().build();
        assertEquals(
                state.withLeadershipAcquired(true).withRunningServers(getServersWithPort(SERVER_IP1, SERVER_IP2)),
                activeWebhooks.getState(WEBHOOK_WITH_A_FEW_LEASES));
        assertEquals(
                state.withLeadershipAcquired(true).withRunningServers(getServersWithPort(SERVER_IP1)),
                activeWebhooks.getState(WEBHOOK_WITH_LEASE));
        assertEquals(
                state.withLeadershipAcquired(true).withRunningServers(newHashSet()),
                activeWebhooks.getState(WEBHOOK_WITH_LOCK));

        assertEquals(
                state.withLeadershipAcquired(false).withRunningServers(newHashSet()),
                activeWebhooks.getState(EMPTY_WEBHOOK));
    }

    private void createWebhook(String webhook) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/leases", WEBHOOK_LEADER_PATH, webhook), "".getBytes());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void createWebhookLock(String webhook, String lockName, String value) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(format("%s/%s/locks/%s", WEBHOOK_LEADER_PATH, webhook, lockName), value.getBytes());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void createWebhookLease(String webhook, String leaseName, String value) {
        String leasePath = format("%s/%s/leases/%s", WEBHOOK_LEADER_PATH, webhook, leaseName);
        try {
            curator.create().creatingParentsIfNeeded().forPath(leasePath, value.getBytes());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:0", server))
                .collect(toSet());
    }
}
