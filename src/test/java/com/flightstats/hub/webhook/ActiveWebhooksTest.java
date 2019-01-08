package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertTrue;

public class ActiveWebhooksTest {
    private static final int HUB_PORT = HubHost.getLocalPort();

    private static final String WEBHOOK_WITH_LEASE = "webhook1";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "webhook4";
    private static final String WEBHOOK_WITH_LOCK = "webhook3";
    private static final String EMPTY_WEBHOOK = "webhook2";

    private static final String SERVER_IP1 = "10.2.1";
    private static final String SERVER_IP2 = "10.2.2";

    private final WebhookLeaderLocks webhookLeaderLocks = mock(WebhookLeaderLocks.class);
    private final ActiveWebhookSweeper activeWebhookSweeper = mock(ActiveWebhookSweeper.class);

    @Test
    public void testInitialize_cleansUpWebhooks() {
        new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper);

        verify(activeWebhookSweeper).cleanupEmpty();
    }

    @Test
    public void testGetServers_returnsSeveralForAWebhook() throws Exception {
        when(webhookLeaderLocks.getServerLeases(WEBHOOK_WITH_A_FEW_LEASES))
                .thenReturn(newHashSet(SERVER_IP2, SERVER_IP1));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper);
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), servers);
    }


    @Test
    public void testGetServers_returnsAnEmptyListIfThereAreNoLeases() throws Exception {
        when(webhookLeaderLocks.getServerLeases(EMPTY_WEBHOOK))
                .thenReturn(newHashSet());

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper);
        Set<String> servers = activeWebhooks.getServers(EMPTY_WEBHOOK);

        assertEquals(newHashSet(), servers);
    }

    @Test
    public void testIsActiveWebhook_isTrueIfWebhookIsPresent() throws Exception {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper);
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LEASE));
    }


    @Test
    public void testIsActiveWebhook_isFalseIfWebhookIsNotPresent() throws Exception{
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper);
        assertFalse(activeWebhooks.isActiveWebhook(EMPTY_WEBHOOK));
    }

    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:%d", server, HUB_PORT))
                .collect(toSet());
    }
}
