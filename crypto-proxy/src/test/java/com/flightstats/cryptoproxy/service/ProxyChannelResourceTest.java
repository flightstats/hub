package com.flightstats.cryptoproxy.service;

import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.Linked;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ProxyChannelResourceTest {

    public static final String DATAHUB_URI = "http://test";
    public static final String URI_QUERY_PARAMS = "param=value";
    private String channelName;
    private UriInfo uriInfo;
    private RestClient restClient;
    private final byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};
    private final static String URI_PATH = "channel/spoon";

    @Before
    public void setup() {
        channelName = "UHF";
        URI requestUri = URI.create("http://testification.com/" + URI_PATH + "?" + URI_QUERY_PARAMS);
        uriInfo = mock(UriInfo.class);
        restClient = mock(RestClient.class);

        when(uriInfo.getRequestUri()).thenReturn(requestUri);
    }

    @Test
    public void testGetValue() throws Exception {
        // GIVEN
        ClientResponse clientResponse = mock(ClientResponse.class);
        HttpHeaders requestHeaders = mock(HttpHeaders.class);
        MultivaluedMapImpl requestHeadersMap = new MultivaluedMapImpl();
        requestHeadersMap.add("foo", "bar");
        MultivaluedMapImpl responseHeaders = new MultivaluedMapImpl();
        responseHeaders.add("foo1", "bar1");

        // WHEN
        when(requestHeaders.getRequestHeaders()).thenReturn(requestHeadersMap);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getHeaders()).thenReturn(responseHeaders);
        when(clientResponse.getEntity(byte[].class)).thenReturn(data);
        when(restClient.get(any(URI.class), any(MultivaluedMap.class))).thenReturn(clientResponse);

        ProxyChannelResource testClass = new ProxyChannelResource("http://test", restClient);
        Response result = testClass.getValue(channelName, uriInfo, requestHeaders);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        MultivaluedMap<String, Object> metadata = result.getMetadata();
        assertEquals(1, metadata.size());
        List<Object> foo = metadata.get("foo1");
        assertEquals(1, foo.size());
        assertEquals("bar1", foo.get(0));
        assertEquals(data, result.getEntity());

    }

    @Test
    public void testInsertValue() throws Exception {
        // GIVEN
        ClientResponse clientResponse = mock(ClientResponse.class);
        HttpHeaders requestHeaders = mock(HttpHeaders.class);
        MultivaluedMapImpl requestHeadersMap = new MultivaluedMapImpl();
        requestHeadersMap.add("foo1", "bar1");
        MultivaluedMapImpl responseHeaders = new MultivaluedMapImpl();
        responseHeaders.add("foo1", "bar1");
        Linked<ValueInsertionResult> linked = mock(Linked.class);

        // WHEN
        when(requestHeaders.getRequestHeaders()).thenReturn(requestHeadersMap);
        when(clientResponse.getHeaders()).thenReturn(responseHeaders);
        when(clientResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(clientResponse.getEntity(Linked.class)).thenReturn(linked);
        when(restClient.post(any(URI.class), any(byte[].class), any(MultivaluedMap.class))).thenReturn(clientResponse);
        when(uriInfo.getPath()).thenReturn(URI_PATH);

        ProxyChannelResource testClass = new ProxyChannelResource(DATAHUB_URI, restClient);
        Response response = testClass.insertValue(channelName, data, requestHeaders, uriInfo);

        // THEN
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        Object object = response.getEntity();
        assertNotNull(linked);
        verify(restClient).post(new URI(DATAHUB_URI + "/" + URI_PATH + "?" + URI_QUERY_PARAMS), data, requestHeadersMap);
    }

    @Test
    public void testStaticCreateResponseBuilderWithoutEntity() {
        // GIVEN
        ClientResponse clientResponse = mock(ClientResponse.class);
        Linked<ValueInsertionResult> linked = mock(Linked.class);

        // WHEN
        when(clientResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(clientResponse.getEntity(Linked.class)).thenReturn(linked);
        when(clientResponse.getHeaders()).thenReturn(new MultivaluedMapImpl());

        Response.ResponseBuilder responseBuilderWithoutEntity = ProxyChannelResource.createResponseBuilderWithoutEntity(clientResponse);

        //THEN
        Response response = responseBuilderWithoutEntity.build();
        assertNull(response.getEntity());
    }
}
