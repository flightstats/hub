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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.mockito.Mockito.*;

public class WebhookManagerUnitTest {
    private final WatchManager watchManager = mock(WatchManager.class);
    private final Dao<Webhook> webhookDao = getWebhookDao();
    private final LastContentPath lastContentPath = mock(LastContentPath.class);
    private final ActiveWebhooks activeWebhooks = mock(ActiveWebhooks.class);
    private final CuratorCluster hubCluster = mock(CuratorCluster.class);
    private final WebhookError webhookError = mock(WebhookError.class);
    private final WebhookContentPathSet webhookContentPathSet = mock(WebhookContentPathSet.class);
    private final Client restClient = mock(Client.class);
    private final InternalWebhookClient webhookClient = new InternalWebhookClient(restClient, hubCluster);

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

        verify(hubCluster, never()).getRandomServers();
        verify(restClient, never()).resource(anyString());
    }

    @Test
    public void testWhenNewWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet());

        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER1);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    public void testWhenInactiveWebhook_getsAddedToServerManagingFewestWebhooks() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(false);

        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER2, SERVER3));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER1);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    public void testWhenNewWebhook_getsAddedToServerManagingFewestWebhooksThatReturnsASuccess() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet());

        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER3);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    public void testWhenWebhookIsManagedByMultipleServers_isRemovedFromAllButOneServer() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1, SERVER2, SERVER3));

        mockWebhookDelete(SERVER1, true);
        mockWebhookDelete(SERVER2, true);
        mockWebhookDelete(SERVER3, true);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), false);

        verify(restClient, times(2)).resource(matches(
                format("(%s|%s|%s)", deleteUrl(SERVER1), deleteUrl(SERVER2), deleteUrl(SERVER3))));
    }

    @Test
    public void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOn() {
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1));

        mockWebhookRun(newArrayList(SERVER1), SERVER1);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), true);

        verify(restClient).resource(runUrl(SERVER1));
    }

    @Test
    public void testWhenWebhookHasChanged_isRunOnAServerThatItWasAlreadyRunningOnAndOnlyOneServer() {
        // TODO: this is a weird case, because it doesn't check that it's trying to run on a server that hasn't been removed;
        // it just picks one that it was running on before it was deleted
        when(activeWebhooks.isActiveWebhook(WEBHOOK_NAME)).thenReturn(true);
        when(activeWebhooks.getServers(WEBHOOK_NAME)).thenReturn(newHashSet(SERVER1, SERVER2, SERVER3));

        mockWebhookRun(newArrayList(SERVER1, SERVER1, SERVER3), SERVER1);
        mockWebhookDelete(SERVER1, true);
        mockWebhookDelete(SERVER2, true);
        mockWebhookDelete(SERVER3, true);

        WebhookManager webhookManager = getWebhookManager();
        webhookManager.manageWebhook(Webhook.builder().name(WEBHOOK_NAME).build(), true);

        verify(restClient, times(2)).resource(matches(
                format("(%s|%s|%s)", deleteUrl(SERVER1), deleteUrl(SERVER2), deleteUrl(SERVER3))));
        verify(restClient, atLeastOnce()).resource(matches(
                format("(%s|%s|%s)", runUrl(SERVER1), runUrl(SERVER2), runUrl(SERVER3))));

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
        verify(hubCluster, never()).getRandomServers();
        verify(restClient, never()).resource(anyString());
    }

    private WebhookManager getWebhookManager() {
        return new WebhookManager(watchManager, webhookDao, lastContentPath, activeWebhooks, webhookError, webhookContentPathSet, webhookClient);
    }

    @SuppressWarnings("unchecked")
    private Dao<Webhook> getWebhookDao() {
        return (Dao<Webhook>) mock(Dao.class);
    }

    private void mockServerWebhookCount(String server, int count) {
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity(String.class)).thenReturn(String.valueOf(count));
        WebResource webResource = mock(WebResource.class);
        when(webResource.get(ClientResponse.class)).thenReturn(response);
        when(restClient.resource(countUrl(server))).thenReturn(webResource);
    }

    private void mockWebhookRun(Collection<String> servers, String successfulServer) {
        servers.forEach(server -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(successfulServer.equals(server) ? 200 : 500);
            WebResource webResource = mock(WebResource.class);
            when(webResource.put(ClientResponse.class)).thenReturn(response);
            when(restClient.resource(runUrl(server))).thenReturn(webResource);
        });
    }

    private void mockWebhookDelete(String server, boolean success) {
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(success? 200 : 500);
        WebResource webResource = mock(WebResource.class);
        when(webResource.put(ClientResponse.class)).thenReturn(response);
        when(restClient.resource(deleteUrl(server))).thenReturn(webResource);
    }

    private String runUrl(String server) {
        return format("http://%s/internal/webhook/run/%s", server, WEBHOOK_NAME);
    }

    private String countUrl(String server) {
        return format("http://%s/internal/webhook/count", server);
    }

    private String deleteUrl(String server) {
        return format("http://%s/internal/webhook/delete/%s", server, WEBHOOK_NAME);
    }
}
