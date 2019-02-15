package com.flightstats.hub.webhook;

import com.flightstats.hub.metrics.MetricsService;
import org.junit.jupiter.api.Test;

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

    private final WebhookLeaderLocks webhookLeaderLocks = mock(WebhookLeaderLocks.class);
    private final MetricsService metricsService = mock(MetricsService.class);

    @Test
    public void testCleanupEmpty_keepsWebhooksWithLeasesAndLocksAndDiscardsOthers() throws Exception {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK, EMPTY_WEBHOOK));

        when(webhookLeaderLocks.getLeasePaths(WEBHOOK_WITH_LEASE)).thenReturn(newArrayList("lease1"));
        when(webhookLeaderLocks.getLeasePaths(WEBHOOK_WITH_A_FEW_LEASES)).thenReturn(newArrayList("lease1b", "lease2b"));
        when(webhookLeaderLocks.getLeasePaths(WEBHOOK_WITH_LOCK)).thenReturn(emptyList());
        when(webhookLeaderLocks.getLeasePaths(EMPTY_WEBHOOK)).thenReturn(emptyList());

        when(webhookLeaderLocks.getLockPaths(WEBHOOK_WITH_LEASE)).thenReturn(newArrayList("lock1"));
        when(webhookLeaderLocks.getLockPaths(WEBHOOK_WITH_A_FEW_LEASES)).thenReturn(emptyList());
        when(webhookLeaderLocks.getLockPaths(WEBHOOK_WITH_LOCK)).thenReturn(newArrayList("lock1b"));
        when(webhookLeaderLocks.getLockPaths(EMPTY_WEBHOOK)).thenReturn(emptyList());

        ActiveWebhookSweeper sweeper = new ActiveWebhookSweeper(webhookLeaderLocks, metricsService);
        sweeper.cleanupEmpty();

        verify(webhookLeaderLocks, never()).deleteWebhookLeader(WEBHOOK_WITH_A_FEW_LEASES);
        verify(webhookLeaderLocks, never()).deleteWebhookLeader(WEBHOOK_WITH_LEASE);
        verify(webhookLeaderLocks, never()).deleteWebhookLeader(WEBHOOK_WITH_LOCK);
        verify(webhookLeaderLocks).deleteWebhookLeader(EMPTY_WEBHOOK);

        verify(metricsService).count("webhook.leaders.cleanup", 1);
    }
}
