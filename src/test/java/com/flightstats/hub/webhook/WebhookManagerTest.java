package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.dao.Dao;
import com.google.common.util.concurrent.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
@Execution(ExecutionMode.SAME_THREAD)
public class WebhookManagerTest {

    @Mock
    private LocalWebhookManager localWebhookManager;
    @Mock
    private WebhookErrorService webhookErrorService;
    @Mock
    private WebhookContentPathSet webhookContentPathSet;
    @Mock
    private InternalWebhookClient webhookClient;
    @Mock
    private WebhookStateReaper webhookStateReaper;
    @Mock
    private LastContentPath lastContentPath;
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
    public void setup() {
        initMocks(this);
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(true);
        HubServices.clear();
    }

    @Test
    void testWhenWebhookIsManagedOnExactlyOneServer_doesNothing() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet("hub-01"));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), any());
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
    }

    @Test
    void testWhenNewWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet());

        when(webhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME)).thenReturn(Optional.of(SERVER1));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(webhookClient).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), any());
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    void testWhenInactiveWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(false);

        when(webhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME)).thenReturn(Optional.of(SERVER1));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(webhookClient).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    void testWhenWebhookIsManagedByMultipleServers_isRemovedFromAllButOneServer() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1, SERVER2, SERVER3));

        when(webhookClient.remove(WEBHOOK_NAME, SERVER1)).thenReturn(true);
        when(webhookClient.remove(WEBHOOK_NAME, SERVER2)).thenReturn(true);
        when(webhookClient.remove(WEBHOOK_NAME, SERVER3)).thenReturn(true);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(webhookClient, times(2)).remove(eq(WEBHOOK_NAME), matches(
                format("(%s|%s|%s)", SERVER1, SERVER2, SERVER3)));
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
        verify(webhookClient, never()).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
    }

    @Test
    void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOn() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1));
        when(webhookClient.runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER1))).thenReturn(Optional.of(SERVER1));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), true);

        verify(webhookClient).runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER1));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOnAndOnlyOneServer() {
        // TODO: this is a weird case, because it doesn't check that it's trying to run on a server that hasn't been removed;
        // it just picks one that it was running on before it was deleted
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1, SERVER2, SERVER3));

        when(webhookClient.runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3))).thenReturn(Optional.of(SERVER1));
        when(webhookClient.remove(WEBHOOK_NAME, SERVER1)).thenReturn(true);
        when(webhookClient.remove(WEBHOOK_NAME, SERVER2)).thenReturn(true);
        when(webhookClient.remove(WEBHOOK_NAME, SERVER3)).thenReturn(true);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), true);

        verify(webhookClient, times(2)).remove(eq(WEBHOOK_NAME), matches(
                format("(%s|%s|%s)", SERVER1, SERVER2, SERVER3)));
        verify(webhookClient).runOnOneServer(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
    }

    @Test
    void testSkipsTagWebhooks() {
        Webhook tagWebhook = Webhook.builder()
                .name(WEBHOOK_NAME)
                .tagUrl("http://some.tag")
                .build();

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(tagWebhook, false);

        verify(activeWebhooks, never()).isActiveWebhook(anyString());
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    void testRegistersServices() {
        getWebhookManager();
        Map<HubServices.TYPE, List<Service>> services = HubServices.getServices();
        assertEquals(2, services.get(HubServices.TYPE.AFTER_HEALTHY_START).size());
        assertEquals(1, services.get(HubServices.TYPE.PRE_STOP).size());
    }

    @Test
    void testIfNotLeaderWontRegisterServices() {
        when(webhookProperties.isWebhookLeadershipEnabled()).thenReturn(false);
        getWebhookManager();
        Map<HubServices.TYPE, List<Service>> services = HubServices.getServices();
        services.forEach((type, svcs) -> assertTrue(type + " has services registered", svcs.isEmpty()));
    }

    private WebhookManager getWebhookManager() {
        return new WebhookManager(
                localWebhookManager,
                webhookErrorService,
                webhookContentPathSet,
                webhookClient,
                webhookStateReaper,
                lastContentPath,
                activeWebhooks,
                webhookProperties,
                watchManager,
                webhookDao);
    }

}
