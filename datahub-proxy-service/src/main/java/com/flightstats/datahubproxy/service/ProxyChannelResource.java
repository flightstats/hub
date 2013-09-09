package com.flightstats.datahubproxy.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.google.common.base.Strings;
import com.google.inject.Inject;
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

    private final String datahubLocation;
    private final RestClient restClient;

    @Inject
    public ProxyChannelResource(@Named("datahub.uri") String datahubLocation, RestClient restClient) {
        this.datahubLocation = datahubLocation;
        this.restClient = restClient;
    }

    @GET
    @Timed(name = "all-channels.fetch")
    @PerChannelTimed(operationName = "fetch", channelNamePathParameter = "channelName")
    @ExceptionMetered
    @Path("/{id}")
    public Response getValue(@PathParam("channelName") String channelName,
                             @Context UriInfo uriInfo,
                             @Context HttpHeaders headers) throws URISyntaxException {

        URI datahubUri = adjustDatahubUri(uriInfo);

        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        ClientResponse clientResponse = restClient.get(datahubUri, requestHeaders);

        byte[] decryptedEntity = clientResponse.getEntity(byte[].class);

        Response.ResponseBuilder responseBuilder = createResponseBuilderWithoutEntity(clientResponse)
                .entity(decryptedEntity);
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

        byte[] encryptedEntity = data;

        URI datahubUri = adjustDatahubUri(uriInfo);
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        ClientResponse clientResponse = restClient.post(datahubUri, encryptedEntity, requestHeaders);

        String entity = clientResponse.getEntity(String.class);
        Response.ResponseBuilder responseBuilder = createResponseBuilderWithoutEntity(clientResponse)
                .type(MediaType.APPLICATION_JSON)
                .entity(entity);
        return responseBuilder.build();
    }

    protected static Response.ResponseBuilder createResponseBuilderWithoutEntity(ClientResponse response) {
        Response.ResponseBuilder rb = Response.status(response.getStatus());
        for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                rb.header(entry.getKey(), value);
            }
        }
        return rb;
    }

    private URI adjustDatahubUri(UriInfo uriInfo) throws URISyntaxException {
        String queryParams = uriInfo.getRequestUri().getQuery();
        queryParams = Strings.isNullOrEmpty(queryParams) ? "" : "?" + queryParams;
        return new URI(datahubLocation + "/" + uriInfo.getPath() + queryParams);
    }
}
