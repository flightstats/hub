package com.flightstats.datahubproxy.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class RestClient {
    private final Client client;

    @Inject
    public RestClient(@Named("restclient.connect.timeout.seconds") int connectTimeoutSec, @Named("restclient.read.timeout.seconds") int readTimeoutSec) {
        client = Client.create();
        client.setConnectTimeout(connectTimeoutSec * 1000);
        client.setReadTimeout(readTimeoutSec * 1000);
    }

    public ClientResponse get(URI uri, MultivaluedMap<String, String> requestHeaders) {
        WebResource.Builder requestBuilder = client.resource(uri).getRequestBuilder();
        applyHeaders(requestBuilder, requestHeaders);

        // Needed since jersey rest client adds some weird default types when not specified
        boolean isAcceptHeaderPresent = null != requestHeaders.get("Accept");
        if (!isAcceptHeaderPresent) {
            requestBuilder.accept(MediaType.WILDCARD);
        }

        return requestBuilder.get(ClientResponse.class);
    }

    public ClientResponse post(URI uri, byte[] data, MultivaluedMap<String, String> requestHeaders) {
        WebResource.Builder requestBuilder = client.resource(uri).getRequestBuilder();
        applyHeaders(requestBuilder, requestHeaders);

        return requestBuilder.post(ClientResponse.class, data);
    }

    private void applyHeaders(WebResource.Builder requestBuilder, MultivaluedMap<String, String> requestHeaders) {
        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                requestBuilder.header(entry.getKey(), value);
            }
        }
    }
}
