package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.Dao;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.mockito.Mockito.*;

public class WebhookManagerTest {
    private final WatchManager watchManager = mock(WatchManager.class);
    private final Dao<Webhook> webhookDao = getWebhookDao();
    private final LastContentPath lastContentPath = mock(LastContentPath.class);
    private final ActiveWebhooks activeWebhooks = mock(ActiveWebhooks.class);
    private final WebhookError webhookError = mock(WebhookError.class);
    private final WebhookContentPathSet webhookContentPathSet = mock(WebhookContentPathSet.class);
    private final InternalWebhookClient webhookClient = mock(InternalWebhookClient.class);

    private static final String SERVER1 = "123.1.1";
    private static final String SERVER2 = "123.2.1";
    private static final String SERVER3 = "123.3.1";

    private static final String WEBHOOK_NAME = "w3bh00k";

    @Test
    public void testWhenWebhookIsManagedOnExactlyOneServer_doesNothing() {
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
    public void testWhenNewWebhook_getsAddedToServerManagingFewestWebhooks() {
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
    public void testWhenInactiveWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(false);

        when(webhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME)).thenReturn(Optional.of(SERVER1));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(webhookClient).runOnServerWithFewestWebhooks(WEBHOOK_NAME);
        verify(webhookClient, never()).runOnOneServer(eq(WEBHOOK_NAME), anyCollectionOf(String.class));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    public void testWhenWebhookIsManagedByMultipleServers_isRemovedFromAllButOneServer() {
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
    public void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOn() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1));
        when(webhookClient.runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER1))).thenReturn(Optional.of(SERVER1));

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), true);

        verify(webhookClient).runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER1));
        verify(webhookClient, never()).remove(eq(WEBHOOK_NAME), anyString());
    }

    @Test
    public void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOnAndOnlyOneServer() {
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
    public void testSkipsTagWebhooks() {
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

    private WebhookManager getWebhookManager() {
        return new WebhookManager(watchManager, webhookDao, lastContentPath, activeWebhooks, webhookError, webhookContentPathSet, webhookClient);
    }

    @SuppressWarnings("unchecked")
    private Dao<Webhook> getWebhookDao() {
        return (Dao<Webhook>) mock(Dao.class);
    }
}
