package com.flightstats.hub.webhook;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActiveWebhookSweeperTest {
    private static final String WEBHOOK_WITH_LEASE = "withLease";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "severalLeases";
    private static final String WEBHOOK_WITH_LOCK = "withLock";
    private static final String EMPTY_WEBHOOK = "gotNothing";

    private final WebhookLeaderServers webhookLeaderServers = mock(WebhookLeaderServers.class);

    @Test
    public void testCleanupEmpty_keepsWebhooksWithLeasesAndLocksAndDiscardsOthers() throws Exception {
        when(webhookLeaderServers.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK, EMPTY_WEBHOOK));

        when(webhookLeaderServers.getLeasePaths(WEBHOOK_WITH_LEASE)).thenReturn(newArrayList("lease1"));
        when(webhookLeaderServers.getLeasePaths(WEBHOOK_WITH_A_FEW_LEASES)).thenReturn(newArrayList("lease1b", "lease2b"));
        when(webhookLeaderServers.getLeasePaths(WEBHOOK_WITH_LOCK)).thenReturn(emptyList());
        when(webhookLeaderServers.getLeasePaths(EMPTY_WEBHOOK)).thenReturn(emptyList());

        when(webhookLeaderServers.getLockPaths(WEBHOOK_WITH_LEASE)).thenReturn(newArrayList("lock1"));
        when(webhookLeaderServers.getLockPaths(WEBHOOK_WITH_A_FEW_LEASES)).thenReturn(emptyList());
        when(webhookLeaderServers.getLockPaths(WEBHOOK_WITH_LOCK)).thenReturn(newArrayList("lock1b"));
        when(webhookLeaderServers.getLockPaths(EMPTY_WEBHOOK)).thenReturn(emptyList());

        ActiveWebhookSweeper sweeper = new ActiveWebhookSweeper(webhookLeaderServers);
        sweeper.cleanupEmpty();

        verify(webhookLeaderServers, never()).deleteWebhookLeader(WEBHOOK_WITH_A_FEW_LEASES);
        verify(webhookLeaderServers, never()).deleteWebhookLeader(WEBHOOK_WITH_LEASE);
        verify(webhookLeaderServers, never()).deleteWebhookLeader(WEBHOOK_WITH_LOCK);
        verify(webhookLeaderServers).deleteWebhookLeader(EMPTY_WEBHOOK);
    }
}
