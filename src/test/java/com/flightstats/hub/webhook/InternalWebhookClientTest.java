package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class InternalWebhookClientTest {

    private static final String WEBHOOK_NAME = "w3bh00k";
    private static final String SERVER1 = "123.1.1";
    private static final String SERVER2 = "123.2.1";
    private static final String SERVER3 = "123.3.1";

    @Mock
    private CuratorCluster hubCluster;
    @Mock
    private Client restClient;
    @Mock
    private LocalHostProperties localHostProperties;
    @Mock
    private ClientResponse clientResponse;
    @Mock
    private WebResource webResource;
    private InternalWebhookClient internalWebhookClient;

    @BeforeEach
    void setup() {
        when(localHostProperties.getScheme()).thenReturn("http://");
        internalWebhookClient = new InternalWebhookClient(hubCluster, restClient, localHostProperties);
    }

    @Test
    void testRunOnServerWithFewestWebhooks() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCount(SERVER2, 6);
        mockServerWebhookCount(SERVER1, 1);
        mockServerWebhookCount(SERVER3, 2);

        mockWebhookRun(SERVER1);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

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

        mockWebhookRun(SERVER3);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

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

        mockWebhookRun(SERVER1);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

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

        mockWebhookRun(SERVER3);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

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

        when(restClient.resource(runUrl("no success"))).thenReturn(webResource);
        when(clientResponse.getStatus()).thenReturn(200);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.empty(), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient).resource(runUrl(SERVER2));

    }

    @Test
    void testRunOnOneServer_runsInOrderUntilOneSucceeds() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockWebhookRun(SERVER1);

        Optional<String> serverRun = internalWebhookClient.runOnOneServer(WEBHOOK_NAME, newArrayList(SERVER2, SERVER1, SERVER3));

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));

    }

    @Test
    void testDelete_success() {
        mockWebhookDelete(SERVER1, true);

        assertTrue(internalWebhookClient.remove(WEBHOOK_NAME, SERVER1));
        verify(restClient).resource(deleteUrl(SERVER1));
    }

    @Test
    void testDelete_fails() {
        mockWebhookDelete(SERVER1, false);

        assertFalse(internalWebhookClient.remove(WEBHOOK_NAME, SERVER1));
        verify(restClient).resource(deleteUrl(SERVER1));
    }

    @Test
    void testDeleteAll_success() {
        mockWebhookDelete(SERVER1, true);
        mockWebhookDelete(SERVER2, true);
        mockWebhookDelete(SERVER3, true);

        List<String> removed = internalWebhookClient.remove(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

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

        List<String> removed = internalWebhookClient.remove(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

        assertEquals(newArrayList(SERVER3), removed);

        verify(restClient).resource(deleteUrl(SERVER1));
        verify(restClient).resource(deleteUrl(SERVER2));
        verify(restClient).resource(deleteUrl(SERVER3));
    }

    private void mockServerWebhookCount(String server, int count) {
        when(clientResponse.getStatus()).thenReturn(200);
        when(clientResponse.getEntity(String.class)).thenReturn(String.valueOf(count));
        when(webResource.get(ClientResponse.class)).thenReturn(clientResponse);
        when(restClient.resource(countUrl(server))).thenReturn(webResource);
    }

    private void mockWebhookRun(String server) {
        when(restClient.resource(runUrl(server))).thenReturn(webResource);
        when(webResource.put(ClientResponse.class)).thenReturn(clientResponse);
        when(clientResponse.getStatus()).thenReturn(200);
    }

    private void mockWebhookDelete(String server, boolean success) {
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(success ? 200 : 500);
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