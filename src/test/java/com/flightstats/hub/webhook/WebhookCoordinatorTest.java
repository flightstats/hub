package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.google.common.util.concurrent.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith({MockitoExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
class WebhookCoordinatorTest {
    @Mock
    private LocalWebhookRunner localWebhookRunner;
    @Mock
    private WebhookErrorService webhookErrorService;
    @Mock
    private WebhookContentInFlight keysInFlight;
    @Mock
    private InternalWebhookClient webhookClient;
    @Mock
    private WebhookStateReaper webhookStateReaper;
    @Mock
    private ClusterCacheDao clusterCacheDao;
    @Mock
    private ActiveWebhooks activeWebhooks;
    @Mock
    private WatchManager watchManager;
    @Mock
    private Dao<Webhook> webhookDao;
    @Mock
    private WebhookProperties webhookProperties;


    private static final String SERVER1 = "123.1.1";
    private static final String SERVER2 = "123.2.1";
    private static final String SERVER3 = "123.3.1";

    private static final String WEBHOOK_NAME = "w3bh00k";

    @BeforeEach
    void setup() {
        initMocks(this);
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        HubServices.clear();
    }

    @Test
    void testWhenWebhookIsManagedOnExactlyOneServer_doesNothing() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet("hub-01"))
                        .build());

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), false);

        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
        verify(webhookClient, never()).runOnOnlyOneServer(eq(WEBHOOK_NAME), anyCollection());
    }

    @Test
    void testWhenNewWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet())
                        .build());

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), false);

        verify(webhookClient).runOnOnlyOneServer(WEBHOOK_NAME, newHashSet());
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
    }

    @Test
    void testWhenInactiveWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(false)
                        .runningServers(newHashSet())
                        .build());

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), false);

        verify(webhookClient).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOnlyOneServer(eq(WEBHOOK_NAME), anyCollection());
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
    }

    @Test
    void testWhenWebhookIsManagedByMultipleServers_isRemovedFromAllButOneServer() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet(SERVER1, SERVER2, SERVER3))
                        .build());

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), false);

        verify(webhookClient).runOnOnlyOneServer(WEBHOOK_NAME, newHashSet(SERVER1, SERVER2, SERVER3));
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
    }

    @Test
    void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOn() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet(SERVER1))
                        .build());

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), true);

        verify(webhookClient).runOnOnlyOneServer(WEBHOOK_NAME, newHashSet(SERVER1));
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
    }

    @Test
    void testWhenWebhookHasChanged_isRunOnOnlyOneServer() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet(SERVER1, SERVER2, SERVER3))
                        .build());


        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getUnpausedWebhook(), true);

        verify(webhookClient).runOnOnlyOneServer(WEBHOOK_NAME, newHashSet(SERVER3, SERVER2, SERVER1));
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
    }

    @Test
    void testWhenWebhookIsPausedButStillRunning_isStopped() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(true)
                        .runningServers(newHashSet(SERVER1))
                        .build());


        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getPausedWebhook(), true);

        verify(webhookClient).stop(WEBHOOK_NAME, newHashSet(SERVER1));
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOnlyOneServer(eq(WEBHOOK_NAME), any());
    }

    @Test
    void testWhenWebhookIsPausedAndNotRunning_doesNothing() {
        when(activeWebhooks.getState(WEBHOOK_NAME)).thenReturn(
                ActiveWebhooks.WebhookState.builder()
                        .leadershipAcquired(false)
                        .runningServers(newHashSet())
                        .build());


        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(getPausedWebhook(), true);

        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOnlyOneServer(eq(WEBHOOK_NAME), any());
    }

    @Test
    void testSkipsTagWebhooks() {
        Webhook tagWebhook = Webhook.builder()
                .name(WEBHOOK_NAME)
                .tagUrl("http://some.tag")
                .build();

        WebhookCoordinator webhookCoordinator = getWebhookCoordinator();
        webhookCoordinator.ensureRunningOnOnlyOneServer(tagWebhook, false);

        verify(activeWebhooks, never()).getState(WEBHOOK_NAME);
        verify(webhookClient, never()).stop(eq(WEBHOOK_NAME), anyCollection());
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOnlyOneServer(eq(WEBHOOK_NAME), any());
    }

    @Test
    void testRegistersServices() {
        getWebhookCoordinator();
        Map<HubServices.TYPE, List<Service>> services = HubServices.getServices();
        assertEquals(2, services.get(HubServices.TYPE.AFTER_HEALTHY_START).size());
        assertEquals(1, services.get(HubServices.TYPE.PRE_STOP).size());
    }

    @Test
    void testIfNotLeaderWontRegisterServices() {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        getWebhookCoordinator();
        Map<HubServices.TYPE, List<Service>> services = HubServices.getServices();
        services.forEach((type, svcs) -> assertTrue(type + " has services registered", svcs.isEmpty()));
    }

    private Webhook getUnpausedWebhook() {
        return Webhook.builder()
                .name(WEBHOOK_NAME)
                .paused(false)
                .build();
    }

    private Webhook getPausedWebhook() {
        return Webhook.builder()
                .name(WEBHOOK_NAME)
                .paused(true)
                .build();
    }

    private WebhookCoordinator getWebhookCoordinator() {
        return new WebhookCoordinator(
                localWebhookRunner,
                webhookErrorService,
                keysInFlight,
                webhookClient,
                webhookStateReaper,
                clusterCacheDao,
                activeWebhooks,
                webhookProperties,
                watchManager,
                webhookDao);
    }

}
