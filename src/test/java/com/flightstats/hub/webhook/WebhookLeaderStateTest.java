package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookLeaderStateTest {
    private static final int HUB_PORT = 8080;

    private static final String WEBHOOK_WITH_LEASE = "webhook1";
    private static final String WEBHOOK_WITH_A_FEW_LEASES = "webhook4";
    private static final String WEBHOOK_WITH_LOCK = "webhook3";
    private static final String EMPTY_WEBHOOK = "webhook2";

    private static final String SERVER_IP1 = "10.2.1";
    private static final String SERVER_IP2 = "10.2.2";

    @Mock
    private WebhookLeaderLocks webhookLeaderLocks;
    @Mock
    private ActiveWebhookSweeper activeWebhookSweeper;
    @Mock
    private LocalHostProperties localHostProperties;
    @Mock
    private WebhookProperties webhookProperties;
    private WebhookLeaderState webhookLeaderState;

    @BeforeEach
    public void setup() {
        webhookLeaderState = new WebhookLeaderState(webhookLeaderLocks, activeWebhookSweeper, webhookProperties, localHostProperties);
    }

    @Test
    void testGetServers_returnsSeveralForAWebhook() {
        when(webhookLeaderLocks.getServerLeases(WEBHOOK_WITH_A_FEW_LEASES))
                .thenReturn(newHashSet(SERVER_IP2, SERVER_IP1));
        when(localHostProperties.getPort()).thenReturn(HUB_PORT);
        Set<String> servers = webhookLeaderState.getServers(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), servers);
    }


    @Test
    void testGetServers_returnsAnEmptyListIfThereAreNoLeases() {
        when(webhookLeaderLocks.getServerLeases(EMPTY_WEBHOOK))
                .thenReturn(newHashSet());
        Set<String> servers = webhookLeaderState.getServers(EMPTY_WEBHOOK);

        assertEquals(newHashSet(), servers);
    }

    @Test
    void testIsActiveWebhook_isTrueIfWebhookIsPresent() {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        assertTrue(webhookLeaderState.isActiveWebhook(WEBHOOK_WITH_LEASE));
    }


    @Test
    void testIsActiveWebhook_isFalseIfWebhookIsNotPresent() {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        assertFalse(webhookLeaderState.isActiveWebhook(EMPTY_WEBHOOK));
    }

    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:%d", server, HUB_PORT))
                .collect(toSet());
    }
}
