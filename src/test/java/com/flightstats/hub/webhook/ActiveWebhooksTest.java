package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;

import java.util.Set;
import java.util.stream.Stream;

import com.flightstats.hub.config.properties.WebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveWebhooksTest {
    private static final int HUB_PORT = HubHost.getLocalPort();

    private static final String WEBHOOK_WITH_LEASE = "webhook1";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "webhook4";
    private static final String WEBHOOK_WITH_LOCK = "webhook3";
    private static final String EMPTY_WEBHOOK = "webhook2";

    private static final String SERVER_IP1 = "10.2.1";
    private static final String SERVER_IP2 = "10.2.2";

    private final WebhookLeaderLocks webhookLeaderLocks = mock(WebhookLeaderLocks.class);
    private final ActiveWebhookSweeper activeWebhookSweeper = mock(ActiveWebhookSweeper.class);
    @Mock
    private WebhookProperties webhookProperties;

    @BeforeEach
    public void setup() {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
    }

    @Test
    void testGetServers_returnsSeveralForAWebhook() throws Exception {
        when(webhookLeaderLocks.getServerLeases(WEBHOOK_WITH_A_FEW_LEASES))
                .thenReturn(newHashSet(SERVER_IP2, SERVER_IP1));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties);
        Set<String> servers = activeWebhooks.getServers(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), servers);
    }


    @Test
    void testGetServers_returnsAnEmptyListIfThereAreNoLeases() throws Exception {
        when(webhookLeaderLocks.getServerLeases(EMPTY_WEBHOOK))
                .thenReturn(newHashSet());

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties);
        Set<String> servers = activeWebhooks.getServers(EMPTY_WEBHOOK);

        assertEquals(newHashSet(), servers);
    }

    @Test
    void testIsActiveWebhook_isTrueIfWebhookIsPresent() throws Exception {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties);
        assertTrue(activeWebhooks.isActiveWebhook(WEBHOOK_WITH_LEASE));
    }


    @Test
    void testIsActiveWebhook_isFalseIfWebhookIsNotPresent() throws Exception{
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties);
        assertFalse(activeWebhooks.isActiveWebhook(EMPTY_WEBHOOK));
    }

    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:%d", server, HUB_PORT))
                .collect(toSet());
    }
}
