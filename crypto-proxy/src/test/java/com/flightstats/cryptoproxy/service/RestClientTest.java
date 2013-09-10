package com.flightstats.cryptoproxy.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RestClientTest {
    private Client client;
    private WebResource webResource;
    private WebResource.Builder requestBuilder;
    private MultivaluedMap<String,String> requestHeaders;
    private final byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};
    private URI uri;

    @Before
    public void setup() throws URISyntaxException {
        client = mock(Client.class);
        webResource = mock(WebResource.class);
        requestBuilder = mock(WebResource.Builder.class);
        uri = new URI("http://test");

        when(client.resource(any(URI.class))).thenReturn(webResource);
        when(webResource.getRequestBuilder()).thenReturn(requestBuilder);

        requestHeaders = new MultivaluedMapImpl();
    }

    @Test
    public void testGet() throws Exception {
        // GIVEN

        RestClient restClient = new RestClient(client);
        ClientResponse clientPostResponse = restClient.get(uri, requestHeaders);

        // THEN
        verify(requestBuilder, times(1)).get(ClientResponse.class);
    }

    @Test
    public void testGetDefaultAccept() throws Exception {
        // GIVEN - no 'Accept' header

        RestClient restClient = new RestClient(client);
        ClientResponse clientResponse = restClient.get(uri, requestHeaders);

        // THEN
        verify(requestBuilder).accept(MediaType.WILDCARD);
    }

    @Test
    public void testGetAccept() throws Exception {
        // GIVEN
        requestHeaders.add("Accept", MediaType.TEXT_PLAIN);

        RestClient restClient = new RestClient(client);
        ClientResponse clientResponse = restClient.get(uri, requestHeaders);

        // THEN
        verify(requestBuilder, never()).accept(any(String.class));
    }

    @Test
    public void testHeaders() throws Exception {
        // GIVEN
        requestHeaders.add("foo", "bar");

        RestClient restClient = new RestClient(client);
        ClientResponse clientGetResponse = restClient.get(uri, requestHeaders);
        ClientResponse clientPostResponse = restClient.post(uri, data, requestHeaders);

        // THEN
        verify(requestBuilder, times(2)).header("foo", "bar");
    }

    @Test
    public void testPost() throws Exception {
        // GIVEN

        RestClient restClient = new RestClient(client);
        ClientResponse clientPostResponse = restClient.post(uri, data, requestHeaders);

        // THEN
        verify(requestBuilder, times(1)).post(ClientResponse.class, data);
    }
}
