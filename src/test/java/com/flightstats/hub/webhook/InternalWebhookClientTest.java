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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalWebhookClientTest {

    private static final String WEBHOOK_NAME = "w3bh00k";
    private static final String SERVER1 = "123.1.1";
    private static final String SERVER2 = "123.2.1";
    private static final String SERVER3 = "123.3.1";
    private static final String SERVER4 = "123.4.1";

    @Mock
    private CuratorCluster hubCluster;
    @Mock
    private Client restClient;
    @Mock
    private LocalHostProperties localHostProperties;
    private InternalWebhookClient internalWebhookClient;

    @BeforeEach
    void setup() {
        when(localHostProperties.getUriScheme()).thenReturn("http://");
        internalWebhookClient = new InternalWebhookClient(hubCluster, restClient, localHostProperties);
        reset(restClient);
    }

    @Test
    void testRunOnServerWithFewestWebhooks() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCountEndpoint(SERVER2, 6);
        mockServerWebhookCountEndpoint(SERVER1, 1);
        mockServerWebhookCountEndpoint(SERVER3, 2);

        mockWebhookRunEndpoint(SERVER1, true);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    void testRunOnServerWithFewestWebhooks_withFailureFromServerWithFewest() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCountEndpoint(SERVER2, 6);
        mockServerWebhookCountEndpoint(SERVER1, 1);
        mockServerWebhookCountEndpoint(SERVER3, 2);

        mockWebhookRunEndpoint(SERVER1, false);
        mockWebhookRunEndpoint(SERVER3, true);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER3), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    void testRunOnServerWithFewestWebhooks_picksOneIfServersAreTied() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCountEndpoint(SERVER2, 6);
        mockServerWebhookCountEndpoint(SERVER1, 1);
        mockServerWebhookCountEndpoint(SERVER3, 1);

        mockWebhookRunEndpoint(SERVER1, true);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    void testRunOnServerWithFewestWebhooks_picksOneEvenIfACountRequestFails() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockFailedServerWebhookCountEndpoint(SERVER1);
        mockServerWebhookCountEndpoint(SERVER2, 6);
        mockServerWebhookCountEndpoint(SERVER3, 2);

        mockWebhookRunEndpoint(SERVER3, true);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.of(SERVER3), serverRun);

        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER2));
    }

    @Test
    void testRunOnServerWithFewestWebhooks_returnsEmptyIfAllServersFailToRun() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER3, SERVER2));
        mockServerWebhookCountEndpoint(SERVER1, 1);
        mockServerWebhookCountEndpoint(SERVER2, 6);
        mockServerWebhookCountEndpoint(SERVER3, 2);

        mockWebhookRunEndpoint(SERVER1, false);
        mockWebhookRunEndpoint(SERVER2, false);
        mockWebhookRunEndpoint(SERVER3, false);

        Optional<String> serverRun = internalWebhookClient.runOnServerWithFewestWebhooks(WEBHOOK_NAME);

        assertEquals(Optional.empty(), serverRun);

        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient).resource(runUrl(SERVER2));

    }

    @Test
    void testRunOnOneServer_runsInOrderUntilOneSucceeds() {
        mockWebhookRunEndpoint(SERVER2, false);
        mockWebhookRunEndpoint(SERVER1, true);

        Optional<String> serverRun = internalWebhookClient.runOnOneServer(WEBHOOK_NAME,
                newArrayList(SERVER2, SERVER1, SERVER3));

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));
    }

    @Test
    void testRunOnOnlyOneServer_ensuresRunningOnSingleServer() {
        mockWebhookRunEndpoint(SERVER2, true);

        Optional<String> serverRun = internalWebhookClient.runOnOnlyOneServer(WEBHOOK_NAME,
                newArrayList(SERVER2));

        assertEquals(Optional.of(SERVER2), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient, never()).resource(stopUrl(SERVER2));
    }

    @Test
    void testRunOnOnlyOneServer_stopsOtherServers() {
        mockWebhookRunEndpoint(SERVER2, true);
        mockWebhookStopEndpoint(SERVER1, false);

        Optional<String> serverRun = internalWebhookClient.runOnOnlyOneServer(WEBHOOK_NAME,
                newArrayList(SERVER2, SERVER1));

        assertEquals(Optional.of(SERVER2), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient, never()).resource(stopUrl(SERVER2));

        verify(restClient).resource(stopUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER1));
    }

    @Test
    void testRunOnOnlyOneServer_triesMultipleServersUntilOneSucceeds() {
        mockWebhookRunEndpoint(SERVER2, false);
        mockWebhookRunEndpoint(SERVER1, true);
        mockWebhookStopEndpoint(SERVER2, false);
        mockWebhookStopEndpoint(SERVER3, true);

        Optional<String> serverRun = internalWebhookClient.runOnOnlyOneServer(WEBHOOK_NAME,
                newArrayList(SERVER2, SERVER1, SERVER3));

        assertEquals(Optional.of(SERVER1), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient, never()).resource(runUrl(SERVER3));

        verify(restClient, never()).resource(stopUrl(SERVER1));
        verify(restClient).resource(stopUrl(SERVER2));
        verify(restClient).resource(stopUrl(SERVER3));
    }

    @Test
    void testRunOnOnlyOneServer_whenRunningServersFailToRun_attemptsToRunOnServerWithFewestWebhooksThatHasNotBeenTried() {
        when(hubCluster.getRandomServers()).thenReturn(newArrayList(SERVER1, SERVER4, SERVER2, SERVER3));
        mockServerWebhookCountEndpoint(SERVER4, 6);
        mockServerWebhookCountEndpoint(SERVER3, 1);
        mockFailedServerWebhookCountEndpoint(SERVER2);
        mockFailedServerWebhookCountEndpoint(SERVER1);

        mockWebhookRunEndpoint(SERVER2, false);
        mockWebhookRunEndpoint(SERVER1, false);
        mockWebhookRunEndpoint(SERVER3, true);

        mockWebhookStopEndpoint(SERVER2, false);
        mockWebhookStopEndpoint(SERVER1, false);

        Optional<String> serverRun = internalWebhookClient.runOnOnlyOneServer(WEBHOOK_NAME,
                newArrayList(SERVER2, SERVER1));

        assertEquals(Optional.of(SERVER3), serverRun);

        verify(restClient).resource(runUrl(SERVER2));
        verify(restClient).resource(runUrl(SERVER1));
        verify(restClient).resource(runUrl(SERVER3));
        verify(restClient, never()).resource(runUrl(SERVER4));

        verify(restClient, never()).resource(stopUrl(SERVER3));
        verify(restClient, never()).resource(stopUrl(SERVER4));
        verify(restClient).resource(stopUrl(SERVER2));
        verify(restClient).resource(stopUrl(SERVER1));
    }

    @Test
    void testStop_success() {
        mockWebhookStopEndpoint(SERVER1, true);

        assertEquals(newArrayList(SERVER1), internalWebhookClient.stop(WEBHOOK_NAME, newArrayList(SERVER1)));
        verify(restClient).resource(stopUrl(SERVER1));
    }

    @Test
    void testStop_fails() {
        mockWebhookStopEndpoint(SERVER1, false);

        assertEquals(newArrayList(), internalWebhookClient.stop(WEBHOOK_NAME, newArrayList(SERVER1)));
        verify(restClient).resource(stopUrl(SERVER1));
    }

    @Test
    void testStopAll_success() {
        mockWebhookStopEndpoint(SERVER1, true);
        mockWebhookStopEndpoint(SERVER2, true);
        mockWebhookStopEndpoint(SERVER3, true);

        List<String> removed = internalWebhookClient.stop(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

        assertEquals(newArrayList(SERVER1, SERVER2, SERVER3), removed);

        verify(restClient).resource(stopUrl(SERVER1));
        verify(restClient).resource(stopUrl(SERVER2));
        verify(restClient).resource(stopUrl(SERVER3));
    }

    @Test
    void testStopAll_withSomeFailure_continuesToCallDeleteOnAllServers() {
        mockWebhookStopEndpoint(SERVER1, false);
        mockWebhookStopEndpoint(SERVER2, false);
        mockWebhookStopEndpoint(SERVER3, true);

        List<String> removed = internalWebhookClient.stop(WEBHOOK_NAME, newArrayList(SERVER1, SERVER2, SERVER3));

        assertEquals(newArrayList(SERVER3), removed);

        verify(restClient).resource(stopUrl(SERVER1));
        verify(restClient).resource(stopUrl(SERVER2));
        verify(restClient).resource(stopUrl(SERVER3));
    }

    private void mockServerWebhookCountEndpoint(String server, int count) {
        ClientResponse clientResponse = mock(ClientResponse.class);
        WebResource webResource = mock(WebResource.class);

        when(clientResponse.getStatus()).thenReturn(200);
        when(clientResponse.getEntity(String.class)).thenReturn(String.valueOf(count));
        when(webResource.get(ClientResponse.class)).thenReturn(clientResponse);
        when(restClient.resource(countUrl(server))).thenReturn(webResource);
    }

    private void mockFailedServerWebhookCountEndpoint(String server) {
        ClientResponse clientResponse = mock(ClientResponse.class);
        WebResource webResource = mock(WebResource.class);

        when(clientResponse.getStatus()).thenReturn(500);
        when(webResource.get(ClientResponse.class)).thenReturn(clientResponse);
        when(restClient.resource(countUrl(server))).thenReturn(webResource);
    }

    private void mockWebhookRunEndpoint(String server, boolean success) {
        ClientResponse clientResponse = mock(ClientResponse.class);
        WebResource webResource = mock(WebResource.class);

        when(clientResponse.getStatus()).thenReturn(success ? 200 : 400);
        when(webResource.put(ClientResponse.class)).thenReturn(clientResponse);
        when(restClient.resource(runUrl(server))).thenReturn(webResource);
    }

    private void mockWebhookStopEndpoint(String server, boolean success) {
        ClientResponse clientResponse = mock(ClientResponse.class);
        WebResource webResource = mock(WebResource.class);

        when(clientResponse.getStatus()).thenReturn(success ? 200 : 500);
        when(webResource.put(ClientResponse.class)).thenReturn(clientResponse);
        when(restClient.resource(stopUrl(server))).thenReturn(webResource);
    }

    private String runUrl(String server) {
        return format("http://%s/internal/webhook/run/%s", server, WEBHOOK_NAME);
    }

    private String countUrl(String server) {
        return format("http://%s/internal/webhook/count", server);
    }

    private String stopUrl(String server) {
        return format("http://%s/internal/webhook/stop/%s", server, WEBHOOK_NAME);
    }
}