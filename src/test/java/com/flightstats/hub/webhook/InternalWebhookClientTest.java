package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorCluster;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class InternalWebhookClientTest {
    private final CuratorCluster hubCluster = mock(CuratorCluster.class);
    private final Client restClient = mock(Client.class);

    private static final String WEBHOOK_NAME = "w3bh00k";
    private static final String SERVER1 = "123.1.1";
    private static final String SERVER2 = "123.2.1";
    private static final String SERVER3 = "123.3.1";

    @Test

    void testRunOnServerWithFewestWebhooks() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER1);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test

    void testRunOnServerWithFewestWebhooks_withFailureFromServerWithFewest() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER3);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER3), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test

    void testRunOnServerWithFewestWebhooks_picksOneIfServersAreTied() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 1);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER1);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test

    void testRunOnServerWithFewestWebhooks_picksOneEvenIfACountRequestFails() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER3);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER3), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test

    void testRunOnServerWithFewestWebhooks_returnsEmptyIfAllServersFailToRun() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), "no success");

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.empty(), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient).resource(runUrl(SERVER2));

    }

    @Test

    void testRunOnOneServer_runsInOrderUntilOneSucceeds() {
        mockWebhookRun(newArrayList(SERVER2, SERVER1, SERVER3), SERVER1);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        Optional<String> serverRun = client.runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER2, SERVER1, SERVER3));

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));

    }

    @Test

    void testDelete_success() {
        mockWebhookDelete(SERVER1, true);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);

        assertTrue(client.remove(WEBHOOK_NAME, SERVER1));
        verify(restClient).resource(deleteUrl(SERVER1));
    }

    @Test

    void testDelete_fails() {
        mockWebhookDelete(SERVER1, false);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);

        assertFalse(client.remove(WEBHOOK_NAME, SERVER1));
        verify(restClient).resource(deleteUrl(SERVER1));
    }

    @Test

    void testDeleteAll_success() {
        mockWebhookDelete(SERVER1, true);
        mockWebhookDelete(SERVER2, true);
        mockWebhookDelete(SERVER3, true);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        List<String> removed = client.remove(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

        assertEquals(newArrayList(SERVER1, SERVER2, SERVER3), removed);

        verify(restClient).resource(deleteUrl(SERVER1));
        verify(restClient).resource(deleteUrl(SERVER2));
        verify(restClient).resource(deleteUrl(SERVER3));
    }

    @Test

    void testDeleteAll_withSomeFailure_continuesToCallDeleteonAllServers() {
        mockWebhookDelete(SERVER1, false);
        mockWebhookDelete(SERVER2, false);
        mockWebhookDelete(SERVER3, true);

        InternalWebhookClient client = new InternalWebhookClient(restClient, hubCluster);
        List<String> removed = client.remove(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

        assertEquals(newArrayList(SERVER3), removed);

        verify(restClient).resource(deleteUrl(SERVER1));
        verify(restClient).resource(deleteUrl(SERVER2));
        verify(restClient).resource(deleteUrl(SERVER3));
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
