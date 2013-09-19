package com.flightstats.cryptoproxy.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.cryptoproxy.security.AESDecryptionCipher;
import com.flightstats.cryptoproxy.security.AESEncryptionCipher;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.client.ClientResponse;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * This resource represents a proxy channel to the DataHub.
 */
@Path("/channel/{channelName}")
public class ProxyChannelResource {

    private final URI datahubLocation;
    private final RestClient restClient;
    private final Provider<AESDecryptionCipher> decryptionCipherProvider;
    private final Provider<AESEncryptionCipher> encryptionCipherProvider;

    @Inject
    public ProxyChannelResource(@Named("datahub.uri") String datahubLocation,
                                RestClient restClient,
                                Provider<AESEncryptionCipher> encryptionCipherProvider,
                                Provider<AESDecryptionCipher> decryptionCipherProvider)
            throws URISyntaxException {
        this.decryptionCipherProvider = decryptionCipherProvider;
        this.encryptionCipherProvider = encryptionCipherProvider;
        this.datahubLocation = new URI(datahubLocation);
        this.restClient = restClient;
    }

    @GET
    @Timed(name = "all-channels.fetch")
    @PerChannelTimed(operationName = "fetch", channelNamePathParameter = "channelName")
    @ExceptionMetered
    @Path("/{id}")
    public Response getValue(@PathParam("channelName") String channelName,
                             @Context UriInfo uriInfo,
                             @Context HttpHeaders headers)
            throws Exception {

        URI datahubUri = adjustDatahubUri(uriInfo);

        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        requestHeaders.remove("Accept-Encoding");
        ClientResponse clientResponse = restClient.get(datahubUri, requestHeaders);

        byte[] decryptedEntity = decryptionCipherProvider.get().decrypt(clientResponse.getEntity(byte[].class));

        Response.ResponseBuilder responseBuilder = createResponseBuilderWithoutEntityOrContentLength(clientResponse)
                .entity(decryptedEntity)
                .header("Content-Length", decryptedEntity.length);
        return responseBuilder.build();
    }

    @POST
    @Timed(name = "all-channels.insert")
    @ExceptionMetered
    @PerChannelTimed(operationName = "insert", channelNamePathParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channelName") final String channelName,
                                final byte[] data,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) throws Exception {

        byte[] encryptedEntity = encryptionCipherProvider.get().encrypt(data);

        URI datahubUri = adjustDatahubUri(uriInfo);
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        requestHeaders.remove("Accept-Encoding");
        ClientResponse clientResponse = restClient.post(datahubUri, encryptedEntity, requestHeaders);

        byte[] entity = clientResponse.getEntity(byte[].class);
        Response.ResponseBuilder responseBuilder = createResponseBuilderWithoutEntityOrContentLength(clientResponse)
                .type(MediaType.APPLICATION_JSON)
                .header("Content-Length", entity.length)
                .entity(entity);
        return responseBuilder.build();
    }

    protected static Response.ResponseBuilder createResponseBuilderWithoutEntityOrContentLength(ClientResponse restClientResponse) {
        Response.ResponseBuilder rb = Response.status(restClientResponse.getStatus());
        for (Map.Entry<String, List<String>> entry : restClientResponse.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                if (!entry.getKey().equalsIgnoreCase("Content-Length")) {
                    rb.header(entry.getKey(), value);
                }
            }
        }
        return rb;
    }

    private URI adjustDatahubUri(UriInfo uriInfo) throws URISyntaxException {
        return uriInfo.getAbsolutePathBuilder()
                .host(datahubLocation.getHost())
                .port(datahubLocation.getPort())
                .build();
    }
}
