package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveWebhooksTest {
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
    private ActiveWebhooks activeWebhooks;

    @BeforeEach
    void setup() {
        activeWebhooks = new ActiveWebhooks(webhookLeaderLocks, activeWebhookSweeper, webhookProperties, localHostProperties);
        reset(webhookLeaderLocks);
    }

    @Test
    void testGetServers_returnsSeveralForAWebhook() {
        when(webhookLeaderLocks.getServerLeases(WEBHOOK_WITH_A_FEW_LEASES))
                .thenReturn(newHashSet(SERVER_IP2, SERVER_IP1));
        when(localHostProperties.getPort()).thenReturn(HUB_PORT);

        ActiveWebhooks.WebhookState state = activeWebhooks.getState(WEBHOOK_WITH_A_FEW_LEASES);

        assertEquals(getServersWithPort(SERVER_IP1, SERVER_IP2), state.getRunningServers());
    }


    @Test
    void testGetServers_returnsAnEmptyListIfThereAreNoLeases() {
        when(webhookLeaderLocks.getServerLeases(EMPTY_WEBHOOK))
                .thenReturn(newHashSet());
        ActiveWebhooks.WebhookState state = activeWebhooks.getState(EMPTY_WEBHOOK);

        assertEquals(newHashSet(), state.getRunningServers());
    }

    @Test
    void testIsLeadershipAcquired_isTrueIfWebhookIsPresent() {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks.WebhookState state = activeWebhooks.getState(WEBHOOK_WITH_LEASE);
        assertTrue(state.isLeadershipAcquired());
    }


    @Test
    void testIsLeadershipAcquired_isFalseIfWebhookIsNotPresent() {
        when(webhookLeaderLocks.getWebhooks())
                .thenReturn(newHashSet(WEBHOOK_WITH_A_FEW_LEASES, WEBHOOK_WITH_LEASE, WEBHOOK_WITH_LOCK));

        ActiveWebhooks.WebhookState state = activeWebhooks.getState(EMPTY_WEBHOOK);
        assertFalse(state.isLeadershipAcquired());
    }


    private Set<String> getServersWithPort(String... servers) {
        return Stream.of(servers)
                .map(server -> format("%s:%d", server, HUB_PORT))
                .collect(toSet());
    }
}
